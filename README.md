# pommes

=pommes= is command line tool for a "pom database". You use it to search for projects and to perform bulk-svn operations.

## Setup

Download the latest application file from Maven Central and add it to your path.

Optionally, you can define a shell function =pg= to invoke =pommes goto= and actually cd into the resulting directory:<verbatim>
function pg {
  pommes goto "$@" && . ~/.pommes.goto
}
</verbatim>

## First steps

<verbatim>
pommes database-add github:mlhartme
pg sushi
</verbatim>

The first fills your database with all projects of the specified user.
The second checks out sushi.

Other examples:

   * =pommes find :-sushi+@/trunk/= to list all trunks with a sushi dependency
   * =pommes mount @/apps/+@/trunk/= to checkout all trunk apps in sales repository

