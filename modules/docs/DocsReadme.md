# Website

The Dokkatoo website is built using [Docusaurus](https://docusaurus.io/), a modern static website
generator.

Some docs are provided by [the documentation subproject](../docs/), as they are processed by Knit.

### Local Development

```shell
./gradlew docusaurusRun
```

This command starts a local development server and opens up a browser window. Most changes are
reflected live without having to restart the server.

### Build

```shell
./gradlew docusaurusBuild
```

This command generates static content into the `build` directory and can be served using any static
contents hosting service.

### Deployment

Using SSH:

```shell
USE_SSH=true yarn deploy
```

Not using SSH:

```shell
GIT_USER=<Your GitHub username> yarn deploy
```

If you are using GitHub pages for hosting, this command is a convenient way to build the website and
push to the `gh-pages` branch.
