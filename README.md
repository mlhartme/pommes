# Pommes

Pommes is a project database tool. It's a command line tool to
* crawl directories, svn, git or artifactory and add matching projects to the database
* search the database by coordinates, dependencies, scm location, etc. 
* perform (bulk-) scm operations, e.g. checkout all projects that match a query.
  
Pommes supports Maven projects with a pom.xml file and php projects with a composer.json file. Note that php support is very basic. 
The name Pommes stands for "many poms".

## Setup

Prerequisites
* Java 8 or higher
* Linux or Mac

Install the application
* download the latest`application.sh` file from [Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.oneandone%22%20AND%20a%3A%22pommes%22) 
* rename it to `pommes`, make it executable and add it to your path.
* Optionally, you can define a shell function `pg` to invoke `pommes goto` and actually cd into the resulting directory:
       function pg {
         pommes goto "$@" && . ~/.pommes.goto
       }
* test it by running `pommes`, you should get a usage message listing available commands
* inspect the configuration file at `~/.pommes.properties` and adjust them to your needs.

Current Limitations
* multi-module projects are not handles properly: only the top-level pom is added to the database.

## Database commands

Pommes uses (Lucene)[http://lucene.apache.org] to store projects. Initially, the database it empty; you have three commands to 
modify it: `database-add` and `database-remove` to add/remove projects, and `database-clear` to reset the database to the initial state.
                                                                         
Use `pommes database-add` to add projects to your database. You can crawl various repositories to for projects.

Local files:

    pommes database-add ~/.m2/repository
    
adds all Maven Poms in your local repository.

Subversion:

    pommes database-add 'svn:https://user:password@svn.yourcompany.com/your/repo/path'

adds alls Project in the specified subversion subtree.

Artifactory:

    pommes database-add 'artifactory:https://user:password@artifactory.yourcompany.com/your/repo/path' 
    
adds all project found in the respective Artifactory repository subtree.

Github:

    pommes database-add github:someuser

adds all Maven Projects of the specified user (or organization) to your database.

Note that `database-add` overwrites projects already in the database, so you don't get duplicate projects from duplicate add commands.
The origin field is used to detect duplicates, the revision field to detect modifications.  

By default, database commands modify your local database only. In addition, you can setup a global database, so others can use it, too. 
In this case, define the `database.global` property and point it to a zip file in a webdav location. Then run your database commands 
with the `-upload` option to modify both the local and the global database. Other users on other machines – with the same 
`database.global` property - automatically initialize their local database from the global one.

You might want to setup a cron job to fill your database automatically every night. Alternatively, you can setup post-commit hooks to 
trigger `database-add` and `database-remove` calls after each committed pom.xml modification.

## Find Command

Pommes stores 7 fields for every project added to the database:
* `origin` - repository uri where the project was imported from
* `revision` - last modified timestamp or content hash (to detect changes when re-adding a project)
* `parent` - parent pom coordinates
* `artifact` - coordinates of this project
* `scm` - where to find this probject in scm
* `dep` - coordinates of dependencies
* `url` - project url

Start with `pommes find foo`, it lists all projects that have a `foo` substring in their scm or artifact field. This query does not specify
any fields to search. 

Next, you can search for specific fields using one or multiple field identifiers - i.e. the first letter of the field name
* `pommes find d:bar` lists projects with a `bar` substring in their dependencies
* `pommes find dp:baz` lists projects with a `baz` substring in their dependency or parent

(Technically, `pommes find foo` is a short-hand for `pommes find :foo`. Fields default to artifact- and scm if they are not specified).

Next, you can combine queries with `+` to list projects matching both conditions. I.e. + means and.
* `pommes find a:puc+s:trunk` lists projects with a `puc` substring in it artifact and `trunk` in its scm.

Finally, you can combine queries with blanks to list projects matching one of the conditions. I.e. blank means or.
* `pommes find d:puc d:pommes` lists projects depending on either `puc` or `pommes`.

## Mount commands

You can use mount commands to perform bulk-svn operations like checking out all trunks.

### fstab 

Before you can use mount commands, you have to create a file `~/.pommes.fstab` in your home directory. This file contains a list of mount points to configure where you want checkouts to show up on your disk. For example

    https://svn.first.org/some/path` /home/foo/pommes/first
    https://svn.second.com/other/path` /home/foo/pommes/second

configures Pommes to checkout projects from `https://svn.first.org/some/path` into the `first` directory, and checkouts from `https://svn.second.com/other/path` into the `second` directory. For example, a project in 'https://svn.first.org/some/path/apps/foo' would be checked out to `/home/foo/pommes/first/apps/foo`

You can edit this file manually, or – if you just want to add a new mount point – you can use `pommes fstab-add url directory`.

Pommes allows to have multiple mount points for that same svn url, as long as they point to different directories. However, it's forbidden to use the same directory or (direct or indirect) subdirectories already used in other mount points.

### Mount

`pommes mount @/trunk/` checks out all trunks. Note that you'll get an error message if you have matches without a mount point configured.

Before changing anything on your disk, the mount command presents a selection of the checkouts to perform, and you can pick one, multiple (separated with blanks) or all of them. Or you can abort.

If a checkout already exists on your disk, it is not touched. This is useful to checkout newly created projects by just re-running your mount command.

### Umount

The `umount` command is equivalent to removing checkouts from your disk with `rm -rf`, but it also checks for uncommitted changes before changing anything. Similar the 'mount' command, it asks before changing anything on your disk.

In addition, you can use the `-stale` option to remove only checkouts that have been removed from the database. For example, if you have all trunks checked out under `/home/foo/trunk`, you can run `pommes umount -stale /home/foo/trunk` to remove checkouts that are no longer used.

    







## Mounting

<verbatim>
pg sushi
</verbatim>

Checks out sushi.

Other examples:

   * =pommes find :-sushi+@/trunk/= to list all trunks with a sushi dependency
   * =pommes mount @/apps/+@/trunk/= to checkout all trunk apps in sales repository

