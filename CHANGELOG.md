## Pommes Changelog

### 3.5.0 (pending)

* build
  * update parent 1.6.1 to 1.6.3
  * update lucene 9.2.0 to 9.8.0
  * update gson 2.9.0 to 2.10.1
  * update application plugin 1.7.0 to 1.8.0
  * update maven-embedded 3.13 to maven-summon 4
  * update jackson-databind
* normalize git urls to avoid conflicting checkout problems
* ls:
  * renamed it to `st`
  * do not abort on pom loading errors, just report them


### 3.4.0 (2022-12-19)

* commands
  * renamed `database-reset` to `index`; dumped `database-add` and `database-remove`
    * don't parse descriptor when revision is the same
  * renamed 'st' to 'ls', and it no longer prints the scm status now; 
    adjusted status indicators to differ from typical scm status indicators 
  * dumped import options, use explicit `index` command instead
* configuration
  * renamed environment variable `POMMES_HOME` to `POMMES_ROOT`, and it now points to the directory containing 
    the former POMMES_HOME (i.e `.pommes`); default is 'Pommes'
  * renamed pommes.properties to config
  * property `checkouts` is gone, it's now configured via `POMMES_ROOT`
  * renamed import to repository
* tailing `.git` in git scm url is now optional
* renamed zone to repository
* fixed NodeRepository to detect raw descriptors in none-root directories
* fixed NodeRepository to check for trunk/branches/tags only for svn nodes (because ~/Projects/branches confused it)
* updates
  * lucene 7.2.1 to 9.2.0
  * glassfish json 1.0.4 to 1.1.4
  * maven-embedded 3.12.2 to 3.13.0
* avoid net.rc loading, because sushi's parser has a problem


### 3.3.0 (2022-06-16)

* status improvements
  * sort entries
  * don't print the project, just the checkout
  * interactively offer to add/relocate where possible
* added "raw projects" that represent projects without meta information
* changed checkout paths: they are now based on the url, not gav. That feels more natural, and it works
  for all projects, even without pom
* support shorter scp-like git ssh urls (used by gitea)
* improved error message for git.getUrl
* implementation
  * rename `Project` class (and deriveds) to `Descriptor`
  * rename `Pom` class to `Project`
  * rename `master` branch to `main`
  * update sushi 3.2.0 to 3.3.0
  * update gson 2.8.0 to 2.9.0
  * update lazy-foss-parent 1.5.0 to 1.5.2


### 3.2.0 (2021-04-09)

* adjust for Java 16: removed --illegal-access=deny from launcher
* updated lazy-foss-parent 1.0.2 to 1.5.0. CAUTION: this compiles pommes for Java 15
* updated junit 4 to 5.7.1
* fixed java version detection if JAVA_TOOL_OPTIONS are set


### 3.1.2 (2018-11-14)

* run with `--illegal-access=deny` unless $JAVA_MODULES is set to `false`
* dependency updates
  * sushi 3.1.6 to 3.2.0
  * parent 1.0.2 to 1.0.3


### 3.1.1 (2018-02-13)

* fixed log output going into `pommes st` output
* fixed `remove -stale` option (the wrong directory was checked)
* dependency updates
  * lucene 6.3.0 to 7.2.1
  * svnkit 1.8.12 to 1.9.0
  * sushi 3.1.3 to 3.1.6
  * autoCd 1.0.0 to 1.0.2
  * inline 1.1.0 to 1.1.1
 
 
### 3.1.0 (2016-12-28)

* renamed `mount` to `checkout` and `umount` to `remove`
* `ls` improvements
  * changed status indicator to be more similar to `svn st`: `?` now flags all projects not in database
  * renamed to `st` because it works similar to svn st now
  * external descriptors:
    `pommes st` now use the `scm` field to match projects with checkouts, previous versions tried to load the project descriptor 
    from the checkout. This change allow you to define projects from external descriptors (i.e. descriptor not stored in the projects),
    e.g. a json file. This is usefull to manage arbitrary checkouts in your pommes directory 
  * ignore the `.database` directory and all `.idea` directories
  * `st` now prints a tip how to fix unknown projects and conflicts
* dumped `-fixscm`, it's always on now; Maven projects now issue a warning if the pom scm does not match the checkout
* improved error handling when crawling repositories
* improved json format: less verbose
* change import options: default is to do nothing; you can trigger imports with '-import-now' or '-import-daily'
* home is no longer created explicitly - use `pommes setup` instead
* configuration changes
  * `$POMMES_PROPERTIES` replaced by `$POMMES_HOME` (default is `$HOME/.pommes`); the properties file is always located in `$POMMES_HOME/pommes.properties` now
  * dumped `database` property, it's always located in `$POMMES_HOME/database` now
  * renamed `mount.root` property to `checkouts`
  * log files moved from `/tmp/pommes.log` to `$POMMES_HOME/logs/pommes.log`
* `pommes-add now supports inline repositories: `inline:`*json*
* fixed Git.server for `git://` urls
* fixed some invalid url exceptions when extracting the git server name
* dependency updates


### 3.0.1 (2016-07-11, not released to Maven Central)

* Improved mount and goto: common actions are merged.
* Improved mount and goto output: print the scm url, not the artifact id.
* Fixed checkout conflict message, it no longer depends on pom.scm.
* Replaced .pommes.goto by autoCd module. 
* Fix npe for checkout actions on a directory without project.


### 3.0.0 (2016-06-10)

* Added ~/.pommes.properties to configure various properties: mount and database directories and to define imports, query macros and format macros.
  Removed POMMES_LOCAL and POMMES_GLOBAL variables, define properties instead. You can change the default location of this file with the new
  POMMES_PROPERTIES variable.
* Dumped ~/.pommes.fstab, all projects are mounted into the configured mount.root property at {scm server}/{ga directory}.
* Added support for git scm.
* Database changes (CAUTION: you have to rebuild your's): updated from Lucene 4.9.1 to 6.0.0. Added revision, scm and url fields.
  Removed g, a, v, ga, dep-ga, dep-ga-ranges and par-ga fields, they were unused. Renamed dep-gav to dep, par-gav to parent,
  gav to artifact, and origin to id. The id is prefixed with a zone string now.
* Much improved query syntax: address fields with their first letter now; added alternatives, separated with blanks;
  added negation starting with '!'; lucene is escape starts with '§' now.
  Added query and format macros.
* Replaced sync mechanism by imports: imports are defined by properties, you specify the zone to import to and the database-add url
  to import from. Replaced sync options -download and -no-download with -import and -no-import. Dumped -upload option,
* Renamed database-clear to database-reset; it also runs imports.
* Renamed list command to ls. It prints an 'M' status if the checkout is not fully committed
  (and pushed for git projects) now, and it prints an 'X' status if the project is unknwon but in scm.
* Improved action selection: return quits without actions now, a applies all actions.
* Dumped -svnuser and -svnpassword options. Specify credentials in the url instead.
  (I've removed them because I don't want special handling for Subversion repository authentication.)
* Improved find argument: use can use query and format macros; the format is appended now; you can merge duplicate lines '-fold'.
  Merged database-export into find. Find now has an optional output argument (which also accepts files now) and -json and -dump
  formats to print pretty-printed or raw json.
* Variables are enclosed in {} now. Renamed {svn} variable to {scm}.
* database-remove with arbitrary queries.
* database-add: Added artifactory support - specify urls prefixed with 'artifactory:';
  * Added github support - specify 'github:' + username;
  * Added bitbucket support - specify 'bitbucket:' + url + project
  * Added file support - specify file urls or file names;
  * Added json support - specify file prefixed with 'json:';
  * Subversion urls have to be prefixed with 'svn:' now; -noBranches changed to %branches; also added %tags (default is false for both)
  * Added -dryrun option.

### 2.4.3 (2016-03-01)

### 2.4.2 (2015-10-05)

* Pommes goto no longer modifies your environment. To get the previous behavior, invoke pommes goto ... && . ~/.pommes.goto.
  Technically, pommes is a normal shell script now (not a shell function).
* Internal change: update sushi 2.8.x to 3.0.0 and inline 1.0.0.
* Added database export command.

### 2.4.1 (2015-01-16)

* Fix == in launcher script, has to be = instead.

### 2.4.0 (2015-01-08)

* Added dependencies to pom objects, you can search for them with "find :-substring". (The users command is just a shortcut for this)
* Configurable output format for find. Also added dependencies to pom objects, so you can output them, too.
* Query syntax: variables replaced context operator.
* Improved setup: 'goto' is 'pommes goto'. To implement this, 'pommes' is a shell function now.
* umount -stale no longer removes active json projects.
