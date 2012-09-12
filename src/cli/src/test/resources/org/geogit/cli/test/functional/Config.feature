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
