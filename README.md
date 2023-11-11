[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.oneandone/pommes)

# Pommes

Pommes is a project checkout manager and database tool, you can use it search for projects and keep their checkouts 
organized on your disk. Here's an example:
    
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
* Git
* Java 17 or higher
* Maven 3.6 or higher

Install the application
* open https://repo1.maven.org/maven2/net/oneandone/pommes/, choose the latest version and download the  `pommes-x.y.z-application.sh` file,
  store it as executable `pommes` file in your path

        cd some-directory-in-your-path
        curl https://repo.maven.apache.org/maven2/net/oneandone/pommes/3.4.0/pommes-3.4.0-application.sh -o pommes
        chmod a+x pommes
    
* run `pommes setup` and follow the instructions. Basically, this sets up Pommes with root directory `~/Projects`, 
  configuration and other files will be stored in `~/Projects/.pommes`. To set up a different directory, pass it as an argument on the 
  command line

## Managing the Database

Pommes maintains a [Lucene](http://lucene.apache.org) database to store project metadata. The database is filled
by indexing so-called repositories. The database is stored in `$POMMES_ROOT/.pommes/database`, it's safe to delete this
directory to wipe the database.

`.pommes/config` defines databases in the form *repository.<name> = <url> {<option>}*
* name is an arbitrary name
* url is the primary definition of the repository, the protocol distinishes the repository type
  * github:<url-of-github-api>
  * gitlab:<url-of-gitlab-server>
  * json:<url-pointing-to-json-file>
  * file:///path-to-local-directory-with checkouts
* options depend on the repository type

Example urls

       repository.mlhartme=github:https://api.github.com %~mlhartme
       repository.company=gitlab:https://gitlab.company.com 

You can configure access token for github and gitlab by storing them in a file $POMMES_ROOT/.pommes/<reponame>-<protocol>.token.
For example, a token in file $POMMES_ROOT/.pommes/github-mlhartme.token will be used to index the above mlhartme repository.

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

Checkout commands (`checkout`, `goto` and `st`) manage checkouts on your disk, e.g. run scm operations on projects that 
match a query. Pommes supports Subversion and Git scms. The root directory for all checkouts is the directory containing
`.pommes`.


### Checkout

`pommes checkout s:/mlhartme` checks out all mlhartme projects.

Before changing anything on your disk, `checkout` presents a selection of the checkouts to perform, and you can pick one, multiple 
(separated with blanks) or all of them. Or you can quit without doing anything.

If a checkout already exists on your disk, it is not touched. This is useful to checkout newly created projects by just re-running your 
checkout command.


### Goto

`pommes goto sushi`

searches the database for `sushi`, offers a selection of matching projects, checks it out if necessary,
and cds into the resulting directory.


### Status

`pommes st` lists all checkouts in the current directory, together with a status marker similar to `git status`. 


## Glossary for reading the source code

Descriptor: references a project, various implementation to load Maven, Json, etc

Project: holds project metadata: gav, parent, dependencies, scm, url

Repository: can be scanned for descriptors

Database: stores projects (in Lucene)


## Usage Message

Project checkout manager and database tool.

Usage:
  'pommes' ['-v'|'-e'] command args*

commands
  'find' ('-output' str)? '-fold'? query ('-' format* | '-json' | '-dump' | '-'MACRO)? 
                        print projects matching this query;
                        append '-json' to print json, '-dump' to print json without formatting;
                        format is a string with placeholders: %c is replace be the current checkout
                        and %FIELD_ID is replaced by the respective field;
                        placeholders can be followed by angle brackets to filter for
                        the enclosed substring or variables;
                        output is a file or URL to write results to, default is the console.
  'checkout' query      checkout matching projects; skips existing checkouts;
                        asks before doing any checkout
  'goto' query          offer selection of matching projects, checks it out when necessary,
                        and cds into the checkout directory
  'st' root?            lists all checkouts under the specified directory (default '.') along with a status:
                        ' ' - checkout is fine
                        '?' - checkout is not in database
                        '!' - checkout in wrong directory
                        '#' - error checking this checkout
  'index' {repo}        re-index the specified (default: all) repositories.
  'setup' ['-batch'] {name'='value}
                        creates '.pommes' directory with initial configuration containing name/values as repositories; 
                        indexes all repositories to create intial database;
                        '.pommes' is created in the directory specified by $POMMES_ROOT, default is ~/Pommes.

fields in the database: (field id is the first letter of the field name.)
  origin      Where this project was loaded from. Used as unique identifier. <repositoryName>:<path>
  revision    Last modified timestamp or content hash of this pom. Used to detect changes.
  parent      Coordinates of the parent project.
  artifact    Coordinates of this project.
  dep         Coordinates of project dependencies.
  scm         Scm location for this project. Where to get the sources.
  url         Url for this project. Where to read about the project.

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
  POMMES_ROOT     directory for manged checkouts and '.pommes'

Home: https://github.com/mlhartme/pommes

