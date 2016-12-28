## Pommes Changelog

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
* Replaced .pommes.goto by setenv module. 
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
