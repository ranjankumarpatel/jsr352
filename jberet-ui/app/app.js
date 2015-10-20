'use strict';

// Declare app level module which depends on views, and components
angular.module('jberetUI',
    ['ui.router', 'jberetUI.jobs', 'jberetUI.jobinstances', 'jberetUI.jobexecutions', 'jberetUI.details', 'jberetUI.version']).

    config(['$urlRouterProvider', function ($urlRouterProvider) {
            $urlRouterProvider.otherwise('/jobs');
        }]);

var jberetui = {
    parseJobParameters: function (keyValues) {
        if (keyValues == null) {
            return null;
        }
        keyValues = keyValues.trim();
        if (keyValues.length == 0) {
            return null;
        }
        var result = {};
        var lines = keyValues.split(/\r\n|\r|\n/g);
        var x;
        for (x in lines) {
            var line = lines[x].trim();
            if (line.length == 0) {
                continue;
            }
            var pair = line.split('=');
            var key = pair[0].trim();
            result[key] = pair.length > 1 ? pair[1].trim() : '';
        }
        return result;
    }
};