# pommes

Pommes is project database tool. It's a command line tool to create a database of Maven projects, search for artifacts 
or dependencies, and maintain project checkouts on your disk.

## Setup

Download the latest application file from Maven Central, rename it to `pommes`, and add it to your path.

Optionally, you can also define a shell function =pg= to invoke =pommes goto= and actually cd into the resulting 
directory:

    function pg {
      pommes goto "$@" && . ~/.pommes.goto
    }

(The following assumes you've defined this function)

## Create a database

Pommes uses Lucense to store Projects. Initially, the database it empty. You can use the `pommes database-clear` command
to get back to an empty database.

Use the `pommes database-add` command to add projects to your database. You can feed it from various repositories:

Local files:

    pommes database-add ~/.m2/repository
    
adds all Maven Poms in your local repository.

Subversion:

    pommes database-add 'svn:https://user:password@svn.yourcompany.com/your/repo/path'\n"

adds alls Project in the specified subversion subtree.

Artifactory:

    pommes database-add 'artifactory:https://user:password@artifactory.yourcompany.com/your/repo/path
    
adds all project found in the respective Artifactory subtree.

Github:

    pommes database-add github:mlhartme

adds all Maven Projects of `mlhartme` to your database.

## Searching

## Mounting

<verbatim>
pg sushi
</verbatim>

Checks out sushi.

Other examples:

   * =pommes find :-sushi+@/trunk/= to list all trunks with a sushi dependency
   * =pommes mount @/apps/+@/trunk/= to checkout all trunk apps in sales repository

