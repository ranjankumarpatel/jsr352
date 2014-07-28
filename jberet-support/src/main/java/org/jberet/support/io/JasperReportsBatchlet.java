/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRAbstractTextDataSource;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXlsxDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.data.XlsDataSource;
import org.jberet.support._private.SupportMessages;

/**
 * A batchlet that generates report using Jasper Reports. Configuration of Jasper Reports is done through either batch
 * properties in job xml and {@code @BatchProperty} injections, or through CDI injections of objects created and
 * configured by other parts of the application. Batch properties generally have higher precedence than CDI-injected
 * counterparts.
 * <p></p>
 * Various report output types supported in Jasper Reports are also supported by this class, such as pdf, html, txt,
 * jrprint, rtf, odt, xml, csv, xls, xlsx, etc.
 * <p></p>
 * Report can be saved to a file, or directed to a {@code java.io.OutputStream}.
 *
 * @since 1.1.0
 */
@Named
@Dependent
public class JasperReportsBatchlet implements Batchlet {
    protected static final String DEFAULT_OUTPUT_TYPE = "pdf";

    /**
     * The resource that provides the data source for generating report. Optional property, and defaults to null.
     * If specified, it may be a URL, a file path, or any resource path that can be loaded by the current application
     * class loader. If this property is not specified, the application should inject appropriate {@code JRDataSource}
     * into {@link #jrDataSourceInstance}.
     * <p></p>
     * {@code JRDataSource} injection allows for more flexible instantiation and configuration, such as setting
     * locale, datePattern, numberPattern, timeZone, recordDelimiter, useFirstRowAsHeader, columnNames, fieldDelimiter,
     * etc, before making the instance available to this class.
     * <p></p>
     * This property has higher precedence than {@link #jrDataSourceInstance} injection.
     * <p></p>
     * @see #jrDataSourceInstance
     */
    @Inject
    @BatchProperty
    protected String resource;

    /**
     * If {@link #resource} is specified, and is a csv resource, this property specifies the delimiter between records,
     * typically new line character (CR/LF). Optional property. See {@code net.sf.jasperreports.engine.data.JRCsvDataSource}
     * for details.
     */
    @Inject
    @BatchProperty
    protected String recordDelimiter;

    /**
     * If {@link #resource} is specified, and is a csv, xls, or xlsx resource, this property specifies whether to use
     * the first row as header. Optional property and valid values are "true" and "false".
     * See {@code net.sf.jasperreports.engine.data.JRCsvDataSource} or
     * {@code net.sf.jasperreports.engine.data.AbstractXlsDataSource} for details.
     */
    @Inject
    @BatchProperty
    protected String useFirstRowAsHeader;

    /**
     * If {@link #resource} is specified, and is a CSV resource, this property specifies the field or column delimiter.
     * Optional property. See {@code net.sf.jasperreports.engine.data.JRCsvDataSource} for details.
     */
    @Inject
    @BatchProperty
    protected String fieldDelimiter;

    /**
     * If {@link #resource} is specified, and is a csv, xls, or xlsx resource, this property specifies an array of
     * strings representing column names matching field names in the report template. Optional property.
     * See {@code net.sf.jasperreports.engine.data.JRCsvDataSource} or
     * {@code net.sf.jasperreports.engine.data.AbstractXlsDataSource}for details.
     */
    @Inject
    @BatchProperty
    protected String[] columnNames;

    /**
     * If {@link #resource} is specified, this property specifies the date pattern string value. Optional property.
     * See {@code net.sf.jasperreports.engine.data.JRAbstractTextDataSource#setDatePattern(java.lang.String)} for details.
     */
    @Inject
    @BatchProperty
    protected String datePattern;

    /**
     * If {@link #resource} is specified, this property specifies the number pattern string value. Optional property.
     * See {@code net.sf.jasperreports.engine.data.JRAbstractTextDataSource#setNumberPattern(java.lang.String)} for details.
     */
    @Inject
    @BatchProperty
    protected String numberPattern;

    /**
     * If {@link #resource} is specified, this property specifies the time zone string value. Optional property.
     * See {@code net.sf.jasperreports.engine.data.JRAbstractTextDataSource#setTimeZone(java.lang.String)} for details.
     */
    @Inject
    @BatchProperty
    protected String timeZone;

    /**
     * If {@link #resource} is specified, this property specifies the locale string value. Optional property.
     * See {@code net.sf.jasperreports.engine.data.JRAbstractTextDataSource#setLocale(java.lang.String)} for details.
     */
    @Inject
    @BatchProperty
    protected String locale;

    /**
     * If {@link #resource} is specified, and is a csv resource, this property specifies the charset name for reading
     * the csv resource. Optional property.
     * See {@code net.sf.jasperreports.engine.data.JRCsvDataSource#JRCsvDataSource(java.io.File, java.lang.String)}
     * for detail.
     */
    @Inject
    @BatchProperty
    protected String charset;

    /**
     * Resource path of the compiled Jasper Reports template (*.jasper file). Required property. It may be a URL,
     * a file path, or any resource path that can be loaded by the current application class loader.
     */
    @Inject
    @BatchProperty
    protected String template;

    /**
     * The format of report output. Optional property and defaults to {@value #DEFAULT_OUTPUT_TYPE}. Valid values are:
     * <ul>
     *     <li>pdf</li>
     *     <li>html</li>
     *     <li>jrprint</li>
     *     <li>txt</li>
     *     <li>rtf</li>
     *     <li>odt</li>
     *     <li>xml</li>
     *     <li>csv</li>
     *     <li>xls</li>
     * </ul>
     */
    @Inject
    @BatchProperty
    protected String outputType;

    /**
     * The file path of the generated report. Optional property and defaults to null. When this property is not
     * specified, the application should inject an {@code java.io.OutputStream} into {@link #outputStreamInstance}.
     * For instance, in order to stream the report to servlet response, a {@code javax.servlet.ServletOutputStream}
     * can be injected into {@link #outputStreamInstance}.
     * <p></p>
     * This property has higher precedence than {@link #outputStreamInstance} injection.
     * <p></p>
     * @see #outputStreamInstance
     */
    @Inject
    @BatchProperty
    protected String outputFile;

    /**
     * Report parameters for generating the report. Optional property and defaults to null. This property can be used
     * to specify string-based key-value pairs as report parameters. For more complex report parameters with object
     * types, use injection into {@link #reportParametersInstance}.
     * <p></p>
     * This property has higher precedence than {@link #reportParametersInstance} injection.
     * <p></p>
     * @see #reportParametersInstance
     */
    @Inject
    @BatchProperty
    protected Map reportParameters;

    /**
     * Optional injection of report output stream, which allows for more control over the output stream creation,
     * sharing, and configuration. The injected {@code OutputStream} is closed at the end of {@link #process()} method.
     * <p></p>
     * @see #outputFile
     */
    @Inject
    protected Instance<OutputStream> outputStreamInstance;

    /**
     * Optional injection of Jasper Reports {@code net.sf.jasperreports.engine.JRDataSource}, which allows for more
     * control over the {@code JRDataSource} creation and configuration.
     * <p></p>
     * @see #resource
     */
    @Inject
    protected Instance<JRDataSource> jrDataSourceInstance;

    /**
     * Optional injection of Jasper Reports report parameters, which allows for more complex, non-string values.
     * <p></p>
     * @see #reportParameters
     */
    @Inject
    protected Instance<Map<String, Object>> reportParametersInstance;

    private InputStream resourceInputStream;

    private String templateFilePath;

    @Override
    public String process() throws Exception {
        InputStream templateInputStream = null;
        OutputStream outputStream = null;

        try {
            if (template == null || !template.toLowerCase().endsWith(".jasper")) {
                //if the template file in *.jrxml, or *.xml format, need to compile the xml design file into
                // serialized "*.jasper" report file
                throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, template, "template (*.jasper)");
            }
            templateInputStream = getTemplateInputStream();

            outputStream = getOutputStream();
            final String ftype = outputType == null ? DEFAULT_OUTPUT_TYPE : outputType.toLowerCase();
            final JRDataSource jrDataSource = getJrDataSource();
            final Map<String, Object> reportParameters1 = getReportParameters();
            if (ftype.equals("pdf")) {
                JasperRunManager.runReportToPdfStream(templateInputStream, outputStream, reportParameters1, jrDataSource);
                outputStream.flush();
            } else if (ftype.equals("html")) {
                JasperRunManager.runReportToHtmlFile(getTemplateFilePath(templateInputStream), outputFile, reportParameters1, jrDataSource);
            } else if (ftype.equals("jrprint")) {
                JasperFillManager.fillReportToFile(getTemplateFilePath(templateInputStream), outputFile, reportParameters1, jrDataSource);
            } else {
                final JasperPrint jasperPrint = JasperFillManager.fillReport(templateInputStream, reportParameters1, jrDataSource);
                // export to common file types: txt, rtf, odt, xml, csv, xls

            }

            return null;
        } finally {
            if (templateInputStream != null) {
                try {
                    templateInputStream.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
            if (resourceInputStream != null) {
                try {
                    resourceInputStream.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
        }

    }

    @Override
    public void stop() throws Exception {
    }

    protected InputStream getTemplateInputStream() {
        return ItemReaderWriterBase.getInputStream(template, false);
    }

    protected OutputStream getOutputStream() throws FileNotFoundException {
        if (outputFile != null) {
            return new FileOutputStream(outputFile);
        }

        // if output needs to be directed to an injected OutputStream
        if (outputStreamInstance != null && !outputStreamInstance.isUnsatisfied()) {
            return outputStreamInstance.get();
        }

        throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, outputFile, "outputFile");
    }

    protected Map<String, Object> getReportParameters() {
        if (reportParameters != null) {
            return reportParameters;
        }

        if (reportParametersInstance != null && !reportParametersInstance.isUnsatisfied()) {
            return reportParametersInstance.get();
        }

        return new HashMap<String, Object>();
    }

    protected JRDataSource getJrDataSource() throws IOException, JRException {
        if (resource != null) {
            final String res = resource.toLowerCase();
            resourceInputStream = ItemReaderWriterBase.getInputStream(resource, false);
            if (res.endsWith(".csv")) {
                final JRCsvDataSource csvDataSource = charset == null ? new JRCsvDataSource(resourceInputStream) :
                        new JRCsvDataSource(resourceInputStream, charset);
                setCommonJRDataSourceProperties(csvDataSource);
                if (useFirstRowAsHeader != null) {
                    csvDataSource.setUseFirstRowAsHeader(Boolean.parseBoolean(useFirstRowAsHeader));
                }
                if (recordDelimiter != null) {
                    csvDataSource.setRecordDelimiter(recordDelimiter);
                }
                if (fieldDelimiter != null) {
                    csvDataSource.setFieldDelimiter(fieldDelimiter.trim().charAt(0));
                }
                if (columnNames != null) {
                    csvDataSource.setColumnNames(columnNames);
                }
                return csvDataSource;
            }
            if (res.endsWith(".xls")) {
                final XlsDataSource xlsDataSource = new XlsDataSource(resourceInputStream);
                setCommonJRDataSourceProperties(xlsDataSource);
                if (columnNames != null) {
                    xlsDataSource.setColumnNames(columnNames);
                }
                if (useFirstRowAsHeader != null) {
                    xlsDataSource.setUseFirstRowAsHeader(Boolean.parseBoolean(useFirstRowAsHeader));
                }
                return xlsDataSource;
            }
            if (res.endsWith(".xlsx")) {
                final JRXlsxDataSource jrXlsxDataSource = new JRXlsxDataSource(resourceInputStream);
                setCommonJRDataSourceProperties(jrXlsxDataSource);
                if (columnNames != null) {
                    jrXlsxDataSource.setColumnNames(columnNames);
                }
                if (useFirstRowAsHeader != null) {
                    jrXlsxDataSource.setUseFirstRowAsHeader(Boolean.parseBoolean(useFirstRowAsHeader));
                }
                return jrXlsxDataSource;
            }
            if (res.endsWith(".xml")) {
                final JRXmlDataSource jrXmlDataSource = new JRXmlDataSource(resourceInputStream);
                setCommonJRDataSourceProperties(jrXmlDataSource);
                return jrXmlDataSource;
            }
            if (res.endsWith(".json")) {
                final JsonDataSource jsonDataSource = new JsonDataSource(resourceInputStream);
                setCommonJRDataSourceProperties(jsonDataSource);
                return jsonDataSource;
            }
            throw SupportMessages.MESSAGES.invalidReaderWriterProperty(null, resource, "resource");
        } else {
            if (jrDataSourceInstance != null && !jrDataSourceInstance.isUnsatisfied()) {
                return jrDataSourceInstance.get();
            }
        }
        return new JREmptyDataSource();
    }

    private String getTemplateFilePath(final InputStream templateInputStream) throws IOException {
        if (templateFilePath != null) {
            return templateFilePath;
        }
        File templateAsFile = new File(template);
        if (templateAsFile.exists()) {
            return templateFilePath = template;
        }

        //the template file path is unknown, need to save it to a file first
        final byte[] buffer = new byte[102400];
        templateAsFile = File.createTempFile("jberet-support-JasperReportsBatchlet", String.valueOf(System.currentTimeMillis()));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(templateAsFile);
            int len;
            while ((len = templateInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
        }
        return templateFilePath = templateAsFile.getPath();
    }

    private void setCommonJRDataSourceProperties(final JRAbstractTextDataSource jrDataSource) {
        if (locale != null) {
            jrDataSource.setLocale(locale);
        }
        if (timeZone != null) {
            jrDataSource.setTimeZone(timeZone);
        }
        if (numberPattern != null) {
            jrDataSource.setNumberPattern(numberPattern);
        }
        if (datePattern != null) {
            jrDataSource.setDatePattern(datePattern);
        }
    }
}