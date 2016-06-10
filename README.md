# Pommes

Pommes is a project database tool. It's a command line tool to
* crawl directories, svn, github, bitbucket or artifactory and add matching projects to the database
* search the database by coordinates, dependencies, scm location, etc. 
* perform (bulk-) scm operations, e.g. checkout all projects that match a query.
  
Pommes supports Maven projects with a pom.xml file and php projects with a composer.json file. Php support is very basic. 
The name Pommes stands for "many poms".

## Setup

Prerequisites
* Java 8 or higher
* Linux or Mac

Install the application
* download the latest `application.sh` file from [Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.oneandone%22%20AND%20a%3A%22pommes%22) 
* rename it to `pommes`, make it executable and add it to your path.
* Define a shell function `pg` to invoke `pommes goto` and actually cd into the resulting directory:
       function pg {
         pommes goto "$@" && . ~/.pommes.goto
       }
* test it by running `pommes`, you should get a usage message listing available commands
* inspect the configuration file at `~/.pommes.properties` and adjust it to your needs.
 
Current Limitations
* doesn't care about git branches
* multi-module projects are not handles properly: only the top-level pom is added to the database.

## Database commands

Pommes uses (Lucene)[http://lucene.apache.org] to store projects. You have three commands to  modify it: `database-add` and `database-remove` 
to add/remove projects, and `database-reset` to reset the database to the initial state.
                                                                         
Use `pommes database-add` to add projects to your database. You can crawl various repositories to for projects.

Local files:

    pommes database-add ~/.m2/repository
    
adds all Maven Poms in your local repository.

Subversion:

    pommes database-add 'svn:https://user:password@svn.yourcompany.com/your/repo/path'

adds alls project in the specified Subversion subtree.

Artifactory:

    pommes database-add 'artifactory:https://user:password@artifactory.yourcompany.com/your/repo/path' 
    
adds all project found in the respective Artifactory repository subtree.

Github:

    pommes database-add github:someuser

adds all projects of the specified user (or organization) to your database.

Note that `database-add` overwrites projects already in the database, so you don't get duplicate projects from duplicate add commands.
The id field is used to detect duplicates, the revision field to detect modifications.  

## Find Command

Pommes stores 7 fields for every project added to the database:
* `id` - zone and repository uri where the project was imported from
* `revision` - last modified timestamp or content hash (to detect changes when re-adding a project)
* `parent` - parent pom coordinates
* `artifact` - coordinates of this project
* `scm` - where to find this probject in scm
* `dep` - coordinates of dependencies
* `url` - project url

Start with `pommes find foo`, it lists all projects that have a `foo` substring in their artifact or scm field. 

Next, you can search for specific fields using one or multiple field identifiers - i.e. the first letter of the field name
* `pommes find d:bar` lists projects with a `bar` substring in their dependencies
* `pommes find dp:baz` lists projects with a `baz` substring in their dependency or parent

(Technically, `pommes find foo` is a short-hand for `pommes find :foo`, and this in turn is a short-hand for `pommes find as:foo`)

Next, you can combine queries with `+` to list projects matching both conditions. I.e. + means and.
* `pommes find a:puc+d:httpcore` lists projects with a `puc` substring in it artifact and `httpcore` in its dependencies.

Finally, you can combine queries with blanks to list projects matching one of the conditions. I.e. blank means or.
* `pommes find d:puc d:pommes` lists projects depending on either `puc` or `pommes`.

TODO: Macros; formats

## Mount commands

You can use mount commands to run scm operations one the projects that match a query. The `mount.root` property defines the root directory
where you want checkouts to show up on your disk. 

### Mount

`pommes mount s:/trunk` checks out all trunks.

Before changing anything on your disk, the mount command presents a selection of the checkouts to perform, and you can pick one, multiple 
(separated with blanks) or all of them. Or you can quit without doing anything.

If a checkout already exists on your disk, it is not touched. This is useful to checkout newly created projects by just re-running your 
mount command.

### Umount

The `umount` command is equivalent to removing checkouts from your disk with `rm -rf`, but it also checks for uncommitted changes before 
changing anything. Similar the `mount` command, it asks before changing anything on your disk.

`umount` has a `-stale` option to remove only checkouts that have been removed from the database. For example, if you have 
all trunks checked out, you can run `pommes umount -stale` to remove checkouts that are no longer used.


### Ls

`pommes ls` lists all checkouts in the current directory, together with a status marker.

### Pg

`pg puc`

searches the database for `puc`, offers a selection of matching projects, checks it out and cds into the resulting directory.

Technically, pg is a shell function that invokes `pommes goto`.

## Advanced 

You can define imports in the properties file:

   import.foo=baruri
   
instructs pommes auto automatically run 

   database-add -zone foo baruri
   
once a day. Or if you invoke it with `-import`. 

This is most usefull with json repositories because they are very fast. To create a json file, run your database-add commands and run

    pommes find -output foo.json "" -json
    
You can also specify a webdav url to automatically upload the file. It will be compressed if the url ends with .gz.

You might want to setup a cron job to run this every night. Alternatively, you can setup post-commit hooks to 
trigger `database-add` and `database-remove` calls after each committed pom.xml modification.

## Usage Message

    Project database tool.
    
    Usage:
      'pommes' ['-v'|'-e'] command sync-options args*
    
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
                            checks for uncommitted changes before selection
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
