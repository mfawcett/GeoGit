Feature: "config" command
    In order to configure geogit
    As a Geogit User
    I want to get and set global settings as well as repository settings on a directory of my choice

  Scenario: Try to set a config value in the current empty directory
    Given I am in an empty directory
     When I run the command "config testing.key value"
     Then it should answer "The config location is invalid"

  Scenario: Try to get a config value in the current empty directory
    Given I am in an empty directory
     When I run the command "config --get testing.key"
     Then it should answer "The config location is invalid"

  Scenario: Try to set and get a global config value in the current empty directory
    Given I am in an empty directory
     When I run the command "config --global testing.global true"
      And I run the command "config --global --get testing.global"
     Then it should answer "true"

  Scenario: Try to set and get a config value in the current repository
    Given I have a repository
     When I run the command "config testing.local true"
      And I run the command "config --get testing.local"
     Then it should answer "true"
  Scenario: Try to get a config value that doesn't exist

    Given I have a repository
     When I run the command "config --global --get doesnt.exist"
     Then it should answer "The section or key is invalid"
     When I run the command "config --get doesnt.exist"
     Then it should answer "The section or key is invalid"

  Scenario: Try to get a config value without specifying key
    Given I have a repository
     When I run the command "config --global --get"
     Then it should answer "No section or name was provided"
     When I run the command "config --get"
     Then it should answer "No section or name was provided"

  Scenario: Try to get a config value using malformed key
    Given I have a repository
     When I run the command "config --global --get test"
     Then it should answer "The section or key is invalid"
     When I run the command "config --get test"
     Then it should answer "The section or key is invalid"

  Scenario: Try to get a config value using the alternate syntax 
    Given I have a repository
     When I run the command "config --global section.key value1"
      And I run the command "config --global section.key"
     Then it should answer "value1"
     When I run the command "config section.key value2"
      And I run the command "config section.key"
     Then it should answer "value2"
