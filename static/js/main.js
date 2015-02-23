"use strict"
var serviceBrokerApp = angular.module('ServiceBrokerApp', []);

serviceBrokerApp.controller('ProvisionedInstanceController', function($scope, $http) {

    $scope.provisionedInstances = {};

    $scope.getProvisionedInstances = function() {
        $http.get("/api/instances").success(function(data) {
            $scope.provisionedInstances = data;
        }).error(function(data, status, headers, config){
            alert("Failed to fetch provisioned instances");
        });
    }
    $scope.getProvisionedInstances();
});

serviceBrokerApp.controller('BoundAppController', function($scope, $http) {

    $scope.boundInstances = {};

    $scope.getBoundInstances = function() {
        $http.get("/api/bindings").success(function(data) {
            $scope.boundInstances = data;
        }).error(function (data, status, headers, config) {
            alert("Failed to fetch bound instances");
        });
    }
    $scope.getBoundInstances();
});

serviceBrokerApp.controller("BrokerDataController", function($scope, $http) {
    $scope.sourceInstance = {};

    $scope.getSourceInstance = function() {
        $http.get("/api/sourceinstance").success(function(data) {
            $scope.sourceInstance = data.sourceInstance;
        }).error(function(data, status, headers, config) {
            alert("Failed to fetch bound instances");
        });
    };

    $scope.getSourceInstance();
});

serviceBrokerApp.controller("SearchController", function($scope, $http) {

    $scope.inProgress = {};

    $scope.failed = {};

    $scope.complete = {};

    $scope.baseFunc = function(searchParam, receiver, failTxt) {
        $http.get("/actions/search/findByState?state=" + searchParam ).success(function(data) {
            if (data._embedded) {
                receiver(data._embedded.actions);
            }
            return {};
        }).error(function(data, status, headers, config) {
            alert ("Failed to fetch " + failTxt);
        });
    };

    $scope.getFailed = function( ) {
        $scope.baseFunc("FAILED", function(d){ $scope.failed = d;}, "failed instances.");
    };
    $scope.getFailed();

    $scope.getInProgress = function( ) {
        $scope.baseFunc("IN_PROGRESS", function(d){ $scope.inProgress = d;}, "in progress instances.");
    };
    $scope.getInProgress();

    $scope.getComplete = function() {
        $scope.baseFunc("COMPLETE", function(d){ $scope.complete = d;}, " complete instances.");
    };
    $scope.getComplete();

});