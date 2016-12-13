[![Build Status](https://secure.travis-ci.org/mlhartme/pommes.png)](https://travis-ci.org/mlhartme/pommes)  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes)

# Pommes

Pommes is a project database tool. 

I use it to keep all the checkouts on my disk organized. Everythings is keep under ~/Pommes with a standardized layout. Pommes can quickly 
add checkouts and jump to existing ones.

Technically, Pommes is a command line tool to maintain a project data base: 
* crawl directories, svn, github, bitbucket or artifactory and add matching projects to the database
* search the database by coordinates, dependencies, scm location, etc. 
* perform (bulk-) scm operations, e.g. checkout all projects that match a query.
* organize checkouts on your disk
  
Pommes supports Maven projects with a pom.xml file and php projects with a composer.json file. Php support is very basic. 
The name Pommes stands for "many poms".

[Changes](https://github.com/mlhartme/pommes/blob/master/CHANGELOG.md)


## Setup

Prerequisites
* Linux or Mac
* Java 8 or higher

Install the application
* download the [latest](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.oneandone%22%20AND%20a%3A%22pommes%22)  `application.sh` file and store it as executable `pommes` file in your path

        curl https://repo.maven.apache.org/maven2/net/oneandone/pommes/3.0.0/pommes-3.0.0-application.sh -o pommes
        chmod a+x pommes
    
* test it by running `pommes`, you should get a usage message
* if you want to enable `pommes goto` to change your current working directory:

        pommes setup >> ~/.bashrc

* create a default configuration with `pommes find`. Inspect the configuration file at `~/.pommes.properties` and adjust it to your needs.
 
Current Limitations
* doesn't care about git branches
* multi-module projects are not handles properly: only the top-level pom is added to the database.

## Database commands

Pommes maintains a [Lucene](http://lucene.apache.org) database to store projects. You'll typically feed it with your corporate projects and 
the open source stuff you regularly use. Once you have projects in the database, you can quickly search for projects or dependencies. And
easily checkout what you need.

You have three commands to modify the database: `database-add` and `database-remove` to add/remove projects, and `database-reset` resets 
the database to the initial empty state.
                                                                         
Use `pommes database-add` to add projects to your database. You can specify various kinds of repositories to crawl for projects.

Local files:

    pommes database-add ~/.m2/repository
    
adds all Maven Poms in your local repository.

Subversion:

    pommes database-add svn:https://user:password@svn.yourcompany.com/your/repo/path

adds all project in the specified Subversion subtree.

Artifactory:

    pommes database-add artifactory:https://user:password@artifactory.yourcompany.com/your/repo/path
    
adds all projects found in the respective Artifactory repository subtree.

Github:

    pommes database-add github:someuser

adds all projects of the specified user (or organization) to your database.

Bitbucket:

    pommes database-add bitbucket:https://bitbucket.yourcompany.com/yourproject
    
adds all projects found in the respective bitbucket project.

Note that `database-add` overwrites existing projects in the database, so you don't get duplicate projects from repeated add commands. 
The id field is used to detect duplicates, the revision field to detect modifications.  

## Find Command

Pommes stores 7 fields for every project added to the database:
* `id` - zone and origin (i.e. repository uri where the project descriptor (usually the pom) was loaded from)
* `revision` - last modified timestamp or content hash (to detect changes when re-adding a project)
* `parent` - parent pom coordinates
* `artifact` - coordinates of this project
* `scm` - where to find this probject in scm
* `dep` - coordinates of dependencies
* `url` - project url

You search your database with the `find` command. First example: `pommes find foo` lists all projects that have a `foo` substring in their artifact or scm field. 
The default is to print the artifact field of matching projects. You can append `-json` to see all fields in json format. 

Next, you can search for specific fields using one or multiple field identifiers - i.e. the first letter of the field name:
* `pommes find d:bar` lists projects with a `bar` substring in their dependencies
* `pommes find dp:baz` lists projects with a `baz` substring in their dependency or parent

(Technically, `pommes find foo` is a short-hand for `pommes find :foo`, and this in turn is a short-hand for `pommes find as:foo`)

You can also prepend fields with `!` for negation. In this case, you should enclose the query argument in single quote, otherwise, the shell 
will expand the `!`. Exmaple: `pommes find '!d:bar'` list all projects that have no dependency with a `bar` substring.

Next, you can combine queries with `+` to list projects matching both conditions. I.e. `+` means *and*.
* `pommes find a:puc+d:httpcore` lists projects with a `puc` substring in its artifact and `httpcore` in its dependencies.

Finally, you can combine queries with blanks to list projects matching one of the conditions. I.e. blank means *or*.
* `pommes find d:puc d:pommes` lists projects depending on either `puc` or `pommes`.

See the usage message below for the full formal query syntax

TODO: Prefix, suffix, macros; formats

## Mount commands

You can use mount commands ('mount', 'umount', 'ls' and 'goto') to run scm operations one projects that match a query. The `mount.root` property defines the root directory where your want checkouts to show up on your disk. Pommes supports Subversion and Git scms.

### Mount

`pommes mount s:/trunk` checks out all trunks.

Before changing anything on your disk, `mount` presents a selection of the checkouts to perform, and you can pick one, multiple (separated with blanks) or all of them. Or you can quit without doing anything.

If a checkout already exists on your disk, it is not touched. This is useful to checkout newly created projects by just re-running your mount command.

### Umount

The `umount` command is equivalent to removing checkouts from your disk with `rm -rf`, but it also checks for uncommitted changes before changing anything. Similar the `mount` command, it asks before changing anything on your disk.

`umount` has a `-stale` option to remove only checkouts that have been removed from the database. For example, if you have 
all trunks checked out, you can run `pommes umount -stale` to remove checkouts that are no longer used.


### Ls

`pommes ls` lists all checkouts in the current directory, together with a status marker.

### Pg

`pg puc`

searches the database for `puc`, offers a selection of matching projects, checks it out and cds into the resulting directory.

Technically, pg is a shell function that invokes `pommes goto` and, if successfull, sources ~/.pommes.goto

## Advanced 

You can define imports in the properties file:

   import.foo=baruri
   
instructs pommes auto automatically run 

   database-add -zone foo baruri
   
once a day (or if you invoke a command with `-import`). 

This is most usefull with json repositories because they are very fast. To create a json file, run your database-add commands and run

    pommes find -output foo.json "" -json
    
You can also specify a webdav url to automatically upload the file. It will be compressed if the url ends with .gz.

You might want to setup a cron job to run this every night. Alternatively, you can setup post-commit hooks to 
trigger `database-add` and `database-remove` calls after each committed pom.xml modification.

## Usage Message

    Project database tool.
    
    Usage:
      'pommes' ['-v'|'-e'] command import-options args*
    
    search commands
      'find' ('-output' str)? '-fold'? query ('-' format* | '-json' | '-dump' | '-'MACRO)? 
                            print projects matching this query;
                            append '-json' to print json, '-dump' to print json without formatting;
                            format is a string with placeholders: %c is replace be the current checkout
                            and %FIELD_ID is replaced by the respective field;
                            place holders can be followed by angle brackets to filter for
                            the enclosed substring or variables;
                            output is a file or URL to write results to, default is the console.
    
    mount commands
      'mount' query         checkout matching projects; skips existing checkouts;
                            offers selection before changing anything on disk;
      'umount' '-stale'? root?
                            remove (optional: stale) checkouts under the specified root directory;
                            a checkout is stale if the project has been removed from the database;
                            offers selection before changing anything on disk;
                            checkouts with uncommitted changes are marked in the list
      'ls' root?            print all checkouts under the specified directory with status markers:
                            M - checkout has modifications or is not pushed
                            C - checkout url does not match the directory pommes would place it in
                            X - checkout is unknown to pommes
                            ? - directory is not a checkout
      'goto' query          offer selection of matching projects, check it out when necessary,
                            and cds into the checkout directory
    
    commands that modify the database
      'database-add' '-delete'? '-dryrun'? '-fixscm'? ('-zone' zone)?  url*
                            add projects found under the specified urls to the database;
                            zone is a prefix added to the id (defaults islocal);
                            overwrites projects with same id;
                            url is a svn url, http url, artifactory url, github url or json url,
                            an option (prefixed with '%') or an exclude (prefixed with '-')  'database-remove' query
                            remove all matching projects
      'database-reset'      deletes the current database and runs any configured imports.
    
    fields in the database: (field id is the first letter of the field name.)
      id          Unique identifier for this pom. Zone + ':' + origin.
      revision    Last modified timestamp or content hash of this pom.
      parent      Coordinates of the parent project.
      artifact    Coordinates of this project.
      scm         Scm location for this project.
      dep         Coordinates of project dependencies.
      url         Url for this project.
    
    import options          how to handle configured imports
      default behavior      import once a day
      '-import'             force import
      '-no-import'          skip import
    
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
      POMMES_PROPERTIES     where to find the properties file
    
    Home: https://github.com/mlhartme/pommes
