[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes)

# Pommes

Pommes is a project checkout manager and database tool, you can use it search for projects and keep their checkouts 
on your disk organized. Here's an example:
    
    ~/Projects $ pommes goto lavender
    [1]   Projects/svn.1and1.org/sales/tools/lavender
    [2] A Projects/github.com/net/oneandone/lavender (git:ssh://git@github.com/mlhartme/lavender.git)
    Please select: 2
    [/Projects/github.com/net/oneandone] git clone ssh://git@github.com/mlhartme/lavender.git lavender
    ~/Projects/github.com/net/oneandone/lavender $ 

This command searches the database for `lavender` projects and lets you choose between the hits. Selecting `1` would 
simply cd into the already existing checkout, choosing `2` also creates the checkout. 

Technically, Pommes is a command line tool that maintains a database with project metadata. Pommes can:
* index repositories (e.g. github or github) to populate the database
* search the database by coordinates, dependencies, scm location, etc. 
* perform (bulk-) checkouts for all projects that match a query.

Pommes detects addition metadata for Maven projects with a `pom.xml` file and php projects with a `composer.json` file. 

The name Pommes stands for "many poms", "project mess", or a german word for french fries.


## See also

[Changes](https://github.com/mlhartme/pommes/blob/master/CHANGELOG.md)

Alternative tools
* https://github.com/gabrie30/ghorg
* https://github.com/x-motemen/ghq
* https://github.com/mthmulders/mcs


## Setup

Prerequisites
* Linux or Mac
* Java 17 or higher
* `git`

Install the application
* open https://repo1.maven.org/maven2/net/oneandone/pommes/, choose the latest version and download the  `pommes-x.y.z-application.sh` file,
  store it as executable `pommes` file in your path

        cd some-directory-in-your-path
        curl https://repo.maven.apache.org/maven2/net/oneandone/pommes/3.4.0/pommes-3.4.0-application.sh -o pommes
        chmod a+x pommes
    
* run `pommes setup` and follow the instructions. Basically, the command creates a directory `~/Projects/.pommes` containing
  a `config` file and a database. (To create `.pommes` in a different directory, define an environment variable
  `POMMES_ROOT` pointing to that directory before runing setup.)

## Managing the Database

Pommes maintains a [Lucene](http://lucene.apache.org) database to store project metadata. The database is filled
by indexing so-called repositories.

`.pommes/config` defines the list of repositories, each is specified by name, url and possibly options.

Available url protocols:
* github
* gitlab

Example configuration

       repository.mlhartme=github:https://api.github.com %~mlhartme
       repository.work=gitlab:https://gitlab.company.com 

## Find Command

Pommes stores the following fields for every project added to the database:
* `id` - zone and origin (i.e. url where the project descriptor (usually the pom) was loaded from)
* `revision` - last modified timestamp or content hash (to detect changes when re-adding a project)
* `parent` - parent pom coordinates; optional, may be missing
* `artifact` - coordinates of this project
* `scm` - where to find this project in a source code management system
* `dep` - coordinates of dependencies; optional list
* `url` - project url; optional, may be missing

You search the database with the `find` command: `pommes find foo` lists all projects that have a `foo` substring in their 
artifact or scm field. The default is to print the artifact field of matching projects. You can append `-json` to get all fields 
in json format instead. 

Next, you can search for specific fields using one or multiple field identifiers. A field identifier is the first letter of the 
field name:
* `pommes find d:bar` lists projects with a `bar` substring in their dependencies
* `pommes find dp:baz` lists projects with a `baz` substring in their dependency or parent

(Technically, `pommes find foo` is a short-hand for `pommes find :foo`, and this in turn is a short-hand for `pommes find as:foo`)

You can also prepend fields with `!` for negation. In this case, you should enclose the query argument in single quote, otherwise, the shell 
will expand the `!`. Example: `pommes find '!d:bar'` list all projects that have no dependency with a `bar` substring.

Next, you can combine queries with `+` to list projects matching both conditions. I.e. `+` means *and*.
* `pommes find a:puc+d:httpcore` lists projects with a `puc` substring in its artifact and `httpcore` in its dependencies.

Finally, you can combine queries with blanks to list projects matching one of the conditions. I.e. blank means *or*.
* `pommes find d:puc d:pommes` lists projects depending on either `puc` or `pommes`.

See the usage message below for the full query syntax.

TODO: Prefix, suffix, macros; formats


## Checkout commands

Checkout commands (`checkout`, `goto` and `ls`) manage checkouts on your disk, e.g. run scm operations on projects that 
match a query. Pommes supports Subversion and Git scms. The root directory for all checkouts is specified by the `checkouts` property
in $POMMES_HOME/pommes.properties.


### Checkout

`pommes checkout s:/mlhartme` checks out all mlhartme projects.

Before changing anything on your disk, `checkout` presents a selection of the checkouts to perform, and you can pick one, multiple 
(separated with blanks) or all of them. Or you can quit without doing anything.

If a checkout already exists on your disk, it is not touched. This is useful to checkout newly created projects by just re-running your 
checkout command.

### Remove

The `remove` command is equivalent to removing checkouts with `rm -rf`, but it also checks for uncommitted changes. Similar to 
the `checkout` command, it asks before changing anything on your disk.

`remove` has a `-stale` option to remove only checkouts that have been removed from the database. For example, if you have 
all trunks checked out, you can run `pommes remove -stale` to remove checkouts that are no longer used.


### List

`pommes ls` lists all checkouts in the current directory, together with a status marker similar to `git status`. 
If a checkout is not in your database, `st` flags it with `?` and prints the command you can run to get it into your database.


### Goto

`pommes goto puc`

searches the database for `puc`, offers a selection of matching projects, checks it out if necessary and cds into the resulting directory.

## Glossary

Descriptor: references a project, various implementation to load Maven, Json, etc

Project: holds project metadata: gav, parent, dependencies, scm, url

Repository: can be scanned for descriptors

Seeds: list of names urls, each url defining a repository.

Database: stores projects (in Lucene)


## Usage Message

    Project checkout manager and database tool.

    Usage:
      'pommes' ['-v'|'-e'] command scan-options args*

    search commands
      'find' ('-output' str)? '-fold'? query ('-' format* | '-json' | '-dump' | '-'MACRO)?
                            print projects matching this query;
                            append '-json' to print json, '-dump' to print json without formatting;
                            format is a string with placeholders: %c is replace be the current checkout
                            and %FIELD_ID is replaced by the respective field;
                            place holders can be followed by angle brackets to filter for
                            the enclosed substring or variables;
                            output is a file or URL to write results to, default is the console.

    checkout commands
      'st' root?            print status of all checkouts under the specified directory:
                            ? - directory is not in database
                            M - checkout has un-tracked files, modifications or is not pushed
                            C - checkout url does not match the directory Pommes would place it in
      'goto' query          offer selection of matching projects, check it out when necessary,
                            and cds into the checkout directory
      'checkout' query      checkout matching projects; skips existing checkouts;
                            offers selection before changing anything on disk
      'remove' '-stale'? root?
                            remove (optional: stale) checkouts under the specified root directory;
                            a checkout is stale if the project has been removed from the database;
                            offers selection before changing anything on disk;
                            checkouts with uncommitted changes are marked in the list - example urls:
                                github:mlhartme
                                svn:https://svn.yourserver.org/some/path -skip/this/subpath
    database commands
      'database-add' '-delete'? '-dryrun'? '-fixscm'? ('-zone' zone)?  url*
                            add projects found under the specified urls to the database;
                            zone is a prefix added to the id (defaults is local);
                            overwrites projects with same id;
                            url is a svn url, http url, artifactory url, github url or json url,
                            an option (prefixed with '%') or an exclude (prefixed with '-')
      'database-remove' query
                            remove all matching projects
      'database-scan'       deletes the current database and re-add all seeds.

    fields in the database: (field id is the first letter of the field name.)
      id          Unique identifier for this project. Zone + ':' + origin.
      revision    Last modified timestamp or content hash of this pom.
      parent      Coordinates of the parent project.
      artifact    Coordinates of this project.
      scm         Scm location for this project.
      dep         Coordinates of project dependencies.
      url         Url for this project.

    scan options            how to handle configured seeds
      default behaviour     no scanning
      '-scan-now'           unconditional scan
      '-scan-daily'         scan if last scan is older than one day

    query syntax
      query     = '@' MACRO | or
      or        = (and (' ' and)*)? index?
      index     = NUMBER
      and       = term ('+' term)*
      term      = field | lucene
      field     = '!'? (FIELD_ID* match)? STR ; match on one of the specified fields (or 'ao' if not specified)
      match     = ':' | '^' | '%' | '='     ; substring, prefix, suffix or string match
      lucene    = '§' STR                   ; STR in Lucene query Syntax: https://lucene.apache.org/core/6_0_1/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html
    and STR may contain the following variables:
      {gav}     = coordinates for current project
      {ga}      = group and artifact of current project
      {scm}     = scm location for current directory

    environment:
      POMMES_HOME     home directory for Pommes

    Home: https://github.com/mlhartme/pommes
