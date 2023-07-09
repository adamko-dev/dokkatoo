# Contributing to Dokkatoo

Thank you for your interest in contributing to
[Dokkatoo](https://github.com/adamko-dev/dokkatoo/)!
We appreciate your help in making this project better.

## Local Development Setup

To get started with local development, follow these steps:

1. Clone the repository:
    ```shell
    git clone https://github.com/adamko-dev/dokkatoo.git
    ```
2. Navigate to the project directory:
    ```shell
    cd dokkatoo
3. Build the project:
    ```shell
    ./gradlew build
    ```

Here are some additional helpful commands:

* Run tests:
    ```shell
    ./gradlew check
    ```
* Publish the project to a project-local directory, to verify publication works:
    ```shell
    ./gradlew publishToTestMavenRepo
    ```
* Update the
  [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator)
  API dump:
    ```shell
    ./gradlew apiDump
    ```

## Adding New Features

When adding new features, it's generally recommended to
[create an issue](https://github.com/adamko-dev/dokkatoo/issues/new)
first before starting any work.
This allows for better discussion and alignment with the project's goals. Creating an issue
can help clarify the purpose and prevent duplicate work.

In some cases, making a PR first can be helpful to demonstrate the desired result and
initiate a discussions. Try to keep the PR small and focused.

If you decide to work on an existing issue, please comment on the issue to let others know that you
are taking it up. This will avoid unnecessary duplication of effort.

## Coding Guidelines

We have the following guidelines for writing code:

* The code style follows the official
  [Kotlin Coding conventions](https://kotlinlang.org/docs/coding-conventions.html#source-file-names),
  with a few differences.

  The most significant difference is that the indent is 2 spaces, not 4.

  The code style is committed in IntelliJ format the repo, in the
  directory [.idea/codeStyles](./.idea/codeStyles).
* Follow
  the [Library creators' guidelines](https://kotlinlang.org/docs/jvm-api-guidelines-introduction.html)
* ABI changes are tracked using
  [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator).
* Ideally all changes should have automated test coverage.

## Commit Guidelines

Each commit should have a clear and descriptive commit message.

## Pull Requests

When submitting a pull request, please follow these guidelines:

* Each PR should have a clear title and description (bullet points are preferred)
  explaining the purpose of the changes.
* Include the issue number(s) related to the PR, if applicable.

PRs will be squashed into a single commit before merging.
This will allow us to maintain a clean commit history.

Thank you for your contribution!
