# Bamboo Plan and Deployment Configuration

This project provides a tool that uses YAML files to specify
how Bamboo plans and deployment projects should be configured.

## Sample configuration files

See the bamboo-configs repository for sample files:

    https://stash.reecenet.org/projects/DE/repos/bamboo-configs

## Credentials and Authentication

Before running this program you need to configure an admin user
to the user the program will run as (ie. your Bamboo credentials)
You do this by creating a `.credentials` file with the following
contents:

    username=<admin user username>
    password=<admin user password>

Don't check this into a repository.

## Building and Running

Build the code with:

    mvn package
    
Run with:

    java -jar target/bamboo-specs-reece-1.0.0.jar permissions permissions.yaml
    java -jar target/bamboo-specs-reece-1.0.0.jar plan plan.yaml
    java -jar target/bamboo-specs-reece-1.0.0.jar deployment deployment-project.yaml

You can test your YAML using the -t switch passed to any of those commands, for example:

    java -jar target/bamboo-specs-reece-1.0.0.jar plan -t plan.yaml

This will just parse the YAML and not deploy it to Bamboo.

The commands all accept multiple yaml files to process:

    java -jar target/bamboo-specs-reece-1.0.0.jar plan configs/plan-*.yaml

## Java SSL keystore fix

If you get this error when running the jar files, you need to add Reece's CA cert to your java keystore:

    Caused by: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPath

    On Ubuntu:
    ----------
    Visit https://bamboo.reecenet.org in Chrome
    Use developer tools - Security - View Certificate - details tab - export
    This will save a copy of the public certificate to a file
    (vicpjdt01.reecenet.org in the example below)
    Import that file using:
    keytool -import -alias vicpjdt01.reecenet.org -keystore cacerts \
    -trustcacerts -file ~dev/vicpjdt01.reecenet.org
    This will prompt for a password; the default is "changeit"

## Controlling Permissions

Create a permissions.yaml file:

    bambooServer: https://bamboo.reecenet.org/bamboo
    projects:
    - plans: [BST-ST, SPAM-IT]
      permissions:
      - groups: [Cyborg_Team]
        users: [islaa, tobind]
        grant: [VIEW, EDIT, BUILD, CLONE, ADMIN]
    - plans: [BST-ST]
      permissions:
      - users: [dooleyj]
        grant: [VIEW]
    deployments:
    - name: Diary Notes Python Shared Service
      permissions:
      - users: [dooleyj, poultonj]
        grant: [VIEW, EDIT]
      environments:
      - names: [Production (AU + NZ), TEST AU, TEST NZ]
        permissions:
        - groups: [Cyborg_Team]
          grant: [VIEW, EDIT, BUILD]
    - name: Customer Prices Service
      permissions:
      - users: [dooleyj, poultonj]
        grant: [VIEW, EDIT]
      environments:
      - names: [Production (AU + NZ)]
        permissions:
        - groups: [Cyborg_Team]
          grant: [VIEW, EDIT, BUILD]

There can be many entries in the permissions: list, each of which
specifies groups and/or users and permissions to grant to them.

The projects list contains plans identified by key pairs (project key, plan
key) so the first list has the ST plan in the BST project, and the IT plan
in the SPAM project. These key pairs are at the end of the URL when
viewing a project's plan, eg:

    https://bamboo.reecenet.org/bamboo/browse/DNSS-DNPSM

The deployments list contains deployments and environments identified by
their label in the Bamboo interface. Note that permissions granted to a
deployment are only for administrating the deployment project settings in
Bamboo and do *not* affect the access controls for each of the environments.

Each permission will be granted to each group and user in each plan, deployment
project or environment for a given permissions entry. So for the first group
above, the complete set of permissions will be granted to the `Cyborg_Team`
group and users `islaa` and `tobind` in the `BST-ST` and `SPAM-IT` plans.

The allowed permissions for each section are:

* **plans**: VIEW, EDIT, BUILD, CLONE, ADMIN
* **deployments**: VIEW, EDIT
* **environments**: VIEW, EDIT, BUILD

The admin user used to make the changes (see Credentials and Authentication) is
hard-coded to be granted admin user permission regardless of the other settings
in the permissions yaml, to prevent that user from having that permission
removed (which would break the program).


## Build and Test Plans

Plans have a lot more options. The required minumum is:

    bambooServer: https://bamboo.reecenet.org/bamboo
    projectKey: BST
    projectName: Bamboo Spec Testing
    planKey: ST
    planName: Spec Testing
    description: This is a test plan for bamboo specs
    
If the Plan or Project do not exist in Bamboo they will be created, so please
double-check that the `projectKey` and `planKey` are correct.

The rest of the configuration is all optional chunks, though some will depend
on others (VCS tasks would require a repository, for example). 

If you have arbitrary variables stored on a plan you may set them as key-value
pairs like so:

    variables:
      major_version_number: 1
      target_name: bamboo-spec-testing

Variables defined here (and others defined by Bamboo for you) may be reference
in SCRIPT task body texts using `${bamboo.major_version_number}` or
`${bamboo.target_name}` using the above example settings.

### Source Repositories

If there are repositories used then include as either linked repositories
(shared between plans):

    linkedRepositories: [Bamboo Spec Test Project, Other Repository]

The linked repository is typically added when a plan is created. Alternatively
you can use a locally (to this plan) defined repository:

    repository:
      name: Bamboo Spec Test Project
      projectKey: SAN
      repositorySlug: bamboo-spec-test-project

The repository above must be in the Reece Stash instance and the configuration
components above come from the repository URL like so:

    https://stash.reecenet.org/projects/<projectKey>/repos/<repositorySlug>/browse

Plan branches are local configurations based on branches in the repository and
the strategy for synchronising the two are controlled with:

    branchManagement:
      createStrategy: MANUALLY

The creation strategy is one of: `MANUALLY`, `ON_PULL_REQUEST`, `ON_NEW_BRANCH`
or `ON_BRANCH_PATTERN`. The last will create on new branches matching a name
pattern regular expression:

    branchManagement:
      createStrategy: ON_BRANCH_PATTERN
      branchPattern: feature/.*

The `issueLinkingEnabled` option enables automatic linking of the plan branch
to the Jira issue related to the repository branch, which is enabled by default.
Cleaning up plan branches is defaulted to 7 days after the repository branch is
deleted, or after 30 days of inactivity in the repository branch. These options
may be modified in the `branchManagement` section:

    branchManagement:
      issueLinkingEnabled: false
      delayCleanAfterDelete: 2
      delayCleanAfterInactivity: 60

### Triggers

Plans may also have a `triggers` section to indicate the specific circumstances
in which they are to be triggered (that is, their tasks should be executed),
say running unit tests after commits to the stash repository:

    triggers:
    - type: AFTER_STASH_COMMIT
      description: Trigger from stash changes

Or perhaps trigger a deploy from a successful build:

    triggers:
    - type: AFTER_SUCCESSFUL_BUILD_PLAN
      description: Deploy main plan branch (master)

If the plan has dependent plans which are to be triggered when
this plan completes they may be specified (as "dependencies"):

    dependencies:
      requiresPassing: true
      plans: [USRSRV-UPSDB]
      
If there are no dependencies you may leave this section out, though if
the plan *previously had dependencies* you will need to explicitly clear
them with:

    dependencies:
      none: true

If you don't then you'll get an error "Plan import is blocked to prevent
deleting your plan dependencies silently."

### Notifications

Notifications on plan completion are supported:

    notifications:
    - when: PLAN_COMPLETED
      slack: https://hooks.slack.com/services/...the rest of the URL...|#cyborg-dev
      recipientGroups: [Cyborg_Team]
      recipientUsers: [dooleyj, poultonj]
      
At least one of the notification targets is required: `slack`,
`recipientGroups` or `recipientUsers`. The `when` values are `PLAN_COMPLETED`,
`PLAN_FAILED`, `DEPLOYMENT_FAILED` and `DEPLOYMENT_FINISHED` which mirror the options of the
same name in the Bamboo UI.

### Stages, Jobs and Tasks

Your plan may have multiple stages, which each have jobs, and each job may have
tasks and *final* tasks (tasks to run even if the other tasks fail).

Stages and jobs may be defined:

    stages:
    - name: Default Stage
      jobs:
      - name: Run Tests
        key: JOB1
        description: Run Python Unit Tests
        requirements:
        - name: system.docker.executable
        - name: DOCKER
        - name: LINUX
        artifacts:
        - name: PACT Contracts
          pattern: "**"
          location: pacts
        - name: Coverage Report
          pattern: "**"
          location: htmlcov

The job key is arbitrary and unique inside a plan. Requirements and artifacts are optional.

The requirements are restrictions on which Bamboo agents may be used to run the plan's jobs.
The actual list of requirements possible is available in the Bamboo UI, though the precise
key to be used in the list above is unclear for the requirements build into Bamboo. For
example, in the Bamboo UI you may select the built-in "Docker" requirement, which in the
list above is "system.docker.executable". The UI also lists "DOCKER" which is represented
in the list above with the same name. I recommend adding the requirement through the UI
and using the "View plan as Bamboo Specs" option under Plan Configuration Actions menu to
determine the actual string to use in the requirements list in YAML.

The job may then have a list of tasks:

    tasks:
    - type: VCS
      description: Checkout Default Repository
      cleanCheckout: true
    - type: SCRIPT
      description: Build docker image
      body: |
        set -ex
        scripts/test_image.sh bamboo/${bamboo.target_name}
    - type: SCRIPT
      description: Run tests
      body: |
        set -ex
        scripts/run_tests.sh
      
Here you can see we refer to the bamboo variable we defined way up above so that
the script body may be the same across multiple projects.

#### VCS Task

The VCS task has a number of options. By default it will check out the default
repository for the plan. If you wish to check out other repositories you may list
them (and optionally include the default repository also):

    - type: VCS
      description: Checkout All Repositories
      defaultRepository: true
      repositories:
      - name: Running Man
      - name: Running Man Properties
        path: properties
      cleanCheckout: true
      
If you wish to force a clean checkout of the repositories on or off use `cleanCheckout`.

#### SCRIPT Tasks

These are pretty simple, just bash scripts that contain a body to run.

#### DOCKER Tasks

Currently only the *run* docker task is supported. It requires the *image* property
to be specified, but also allows all the other options:

    - type: DOCKER
      description: Run unit tests
      image: dockerrepo.reecenet.org:4433/cyborg/tox-tests
      workingDirectory: /app
      environmentVariables: PACT_DIR=/app/pacts
      volumeMappings:
      - local: ${bamboo.working.directory}
        container: /app
      command: tox

Also supported is running docker containers in the background with port mappings:

    - type: DOCKER
      description: Run unit tests
      image: dockerrepo.reecenet.org:4433/cyborg/some-server
      workingDirectory: /app
      portMappings:
      - local: 8080
        container: 8001
      detach: true
      waitToStart: true


### Final Tasks

Final tasks are tasks that are always run after the other tasks, regardless of whether
they were successful. These could be cleanup tasks, or more commonly including a
JUnit parser to parse the results of the tests which may have failed:

    finalTasks:
    - type: JUNIT
      description: Include XML test results
      resultFrom: "**/unittest-report/xml/*.xml"
      
## Reusable Jobs

If you have the same Job run across many different plans you can craft a YAML file that
contains *just* the Job specification, for example "include/library-unit-tests.yaml":

    name: Run Library Unit Tests
    key: UT
    description: Run tox unit tests and check setup.py version against existing git tags
    requirements:
    - name: system.docker.executable
    tasks:
    - type: VCS
    ...

And then in your plan YAML file you may include that (noting that you may also declare
other plan-specific jobs):

    stages:
    - name: Default Stage
      jobs:
      - include: include/library-unit-tests.yaml
      - name: Build package
        key: BUILD
        description: Build Python package
        requirements:
        ...

In this example the include file is in a separate subdirectory - this makes it easier
to use globbing to process multiple files in a single directory, like:

  java -jar target/bamboo-specs-reece-1.0.5.jar plan bamboo-configs/Cyborg/unit-tests/*.yaml


## Deployment Projects

Deployment projects look a lot like build and test plans, and even share some of the
same sections, but do have a different preamble and structure.

### Preamble

At the top of the file you need to identify the deployment by *name*, and then the
build plan that it belongs to:

    bambooServer: https://bamboo.reecenet.org/bamboo
    name: Diary Notes Python Shared Service
    buildProject: DNSS
    buildPlan: DNPSDB
    description: This is a deployment plan for the Diary Notes Python Shared Service

You should also define the release naming scheme:

    releaseNaming:
      pattern: ${bamboo.version_major_number}.${bamboo.buildNumber}
      
If you would like to set variables across all environments you can set variables in
the preamble:

    variables:
      target_name: diary-notes-service

This will have the effect of setting the variable in each of the environments (Bamboo
does not offer variables at this level).

### Environments Structure

This is a list of environments that you will deploy to. Each
environment will have a name and description followed by requirements, notifications,
variables and tasks that are constructed exactly the same as in build plans. So for example:
    
    environments:
    - environment: Production (AU + NZ)
      description: Deployment plan for the Diary Notes Python Shared Service to production
      requirements:
      - name: system.docker.executable
      notifications:
      - when: DEPLOYMENT_FINISHED
        slack: https://hooks.slack.com/services/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|#cyborg-dev
      variables:
        deployment_script: cutover.py
      tasks:
      - type: VCS
        description: Running Man
        repositories:
        - name: Running Man
        - name: Running Man Properties
          path: properties
      - type: SCRIPT
        description: Cutover Blue to Green - Training
        body: |
          ${deployment_script} ${bamboo.target_name} training_nz ${bamboo.version_major_number}.${bamboo.buildNumber}
          ${deployment_script} ${bamboo.target_name} training_au ${bamboo.version_major_number}.${bamboo.buildNumber}
      - type: SCRIPT
        description: Cutover Blue to Green - Production
        body: |
          ${deployment_script} ${bamboo.target_name} prod_au ${bamboo.version_major_number}.${bamboo.buildNumber}
          ${deployment_script} ${bamboo.target_name} prod_nz ${bamboo.version_major_number}.${bamboo.buildNumber}
      triggers:
      - type: AFTER_SUCCESSFUL_BUILD_PLAN
        description: Deploy main plan branch (master)
