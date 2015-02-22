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