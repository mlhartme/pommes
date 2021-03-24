[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes)

# Pommes

Pommes is a project checkout manager and database tool, you can use it to keep the checkouts on your disk organized. Here's an example:
    
    ~/Projects $ pommes goto lavender
    [1]   Projects/svn.1and1.org/sales/tools/lavender
    [2] A Projects/github.com/net/oneandone/lavender (git:ssh://git@github.com/mlhartme/lavender.git)
    Please select: 2
    [/Projects/github.com/net/oneandone] git clone ssh://git@github.com/mlhartme/lavender.git lavender
    ~/Projects/github.com/net/oneandone/lavender $ 

This searches the database for `lavender` projects and lets you choose between the hits. Selecting `1` would simply cd into the
already existing checkout, choosing `2` also creates the checkout. Another use case: if you keep all release of your project
a database, you can search when a given dependency as used.

Technically, Pommes is a command line tool that maintains a database with project metadata. Pommes can:
* crawl directories, svn, github, bitbucket or artifactory and add matching projects to the database
* search the database by coordinates, dependencies, scm location, etc. 
* perform (bulk-) scm operations, e.g. checkout all projects that match a query.
  
Pommes supports Maven projects with a pom.xml file, php projects with a composer.json file, and projects defined with by Pommes-Json. 
Php support is very basic. 

The name Pommes stands for "many poms".


[Changes](https://github.com/mlhartme/pommes/blob/master/CHANGELOG.md)


## Setup

Prerequisites
* Linux or Mac
* Java 8 or higher

Install the application
* download the [latest](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.oneandone%22%20AND%20a%3A%22pommes%22)  `application.sh` file 
and store it as executable `pommes` file in your path

        cd some-directory-in-your-path
        curl https://repo.maven.apache.org/maven2/net/oneandone/pommes/3.1.0/pommes-3.1.0-application.sh -o pommes
        chmod a+x pommes
    
* run `pommes setup` and follow the instructions
 
Current Limitations:
* doesn't care about git branches
* crawling multi-module projects will only add the top-level pom to the database


## Database commands

Pommes maintains a [Lucene](http://lucene.apache.org) database to store projects. You'll typically feed it with your corporate projects and 
the open source stuff you regularly use. 

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

Note that `database-add` overwrites existing projects in the database, so you don't get duplicate projects from repeated invokations. 
The id field is used to detect duplicates, the revision field to detect modifications.  

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

Checkout commands (`checkout`, `remove`, `st` and `goto`) manage checkouts on your disk, e.g. run scm operations on projects that 
match a query. Pommes supports Subversion and Git scms. The root directory for all checkouts is specified by the `checkouts` property
in $POMMES_HOME/pommes.properties.


### Checkout

`pommes checkout s:/trunk` checks out all trunks.

Before changing anything on your disk, `checkout` presents a selection of the checkouts to perform, and you can pick one, multiple 
(separated with blanks) or all of them. Or you can quit without doing anything.

If a checkout already exists on your disk, it is not touched. This is useful to checkout newly created projects by just re-running your 
checkout command.

### Remove

The `remove` command is equivalent to removing checkouts with `rm -rf`, but it also checks for uncommitted changes. Similar to 
the `checkout` command, it asks before changing anything on your disk.

`remove` has a `-stale` option to remove only checkouts that have been removed from the database. For example, if you have 
all trunks checked out, you can run `pommes remove -stale` to remove checkouts that are no longer used.


### Status

`pommes st` lists all checkouts in the current directory, together with a status marker similar to `svn st`. 

This command is useful to cleanup your checkouts. E.g. you can see where you have uncommitted files: run it on a subtree with 
many checkouts before you run `rm -rf`

If a checkout is not in your database, `st` flags it with `?` and prints the command you can run to get it into your database.


### Goto

`pommes goto puc`

searches the database for `puc`, offers a selection of matching projects, checks it out if necessary and cds into the resulting directory.


## Advanced 

You can define imports in the properties file:

   import.foo=baruri
   
instructs Pommes auto automatically run 

   database-add -zone foo baruri
   
during `setup`, `database-reset` or if you invoke Pommes with `-import-now` or `-import-daily`. 

This is most usefull with json repositories. To create a json file, run your database-add commands and run

    pommes find -output foo.json "" -json
    
to create a json repository `foo.json`. You can also specify a webdav url to automatically upload the file. 
It will be compressed if the url ends with .gz.

You might want to setup a cron job to run this every night. Alternatively, you can setup post-commit hooks to 
trigger `database-add` and `database-remove` calls after each committed pom.xml modification.


## Usage Message

    Project checkout manager and database tool.
    
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
    
    checkout commands
      'st' root?            print all checkouts under the specified directory with status markers:
                            ? - directory is not an unknown project
                            M - checkout has untracked files, modifications or is not pushed
                            C - checkout url does not match the directory Pommes would place it in
      'goto' query          offer selection of matching projects, check it out when necessary,
                            and cds into the checkout directory
      'checkout' query      checkout matching projects; skips existing checkouts;
                            offers selection before changing anything on disk;
      'remove' '-stale'? root?
                            remove (optional: stale) checkouts under the specified root directory;
                            a checkout is stale if the project has been removed from the database;
                            offers selection before changing anything on disk;
                            checkouts with uncommitted changes are marked in the list
    
    database commands
      'database-add' '-delete'? '-dryrun'? '-fixscm'? ('-zone' zone)?  url*
                            add projects found under the specified urls to the database;
                            zone is a prefix added to the id (defaults is local);
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
      default behaviour     no imports
      '-import-now'         unconditional import
      '-import-daily'       import if last import is older than one day
    
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
    
