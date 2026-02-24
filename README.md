# Discogs-CTL

<div>
    <p>
        <em>A CLI tool for Discogs to browse and cross-reference collections, shops and wantlists between users.</em>
    </p>
    <p>
        <img src="https://img.shields.io/github/license/ezienecker/discogs-ctl?style=default&logo=opensourceinitiative&logoColor=white&color=0080ff" alt="license">
        <img src="https://img.shields.io/github/last-commit/ezienecker/discogs-ctl?style=default&logo=git&logoColor=white&color=0080ff" alt="last-commit">
        <img src="https://img.shields.io/github/languages/top/ezienecker/discogs-ctl?style=default&color=0080ff" alt="repo-top-language">
        <img src="https://img.shields.io/github/languages/count/ezienecker/discogs-ctl?style=default&color=0080ff" alt="repo-language-count">
    </p>
</div>

## What it does

- **Browse collections, shops & wantlists** – Fetch any Discogs user's inventory and display it in the terminal as a
  table, detail view or JSON.
- **Cross-reference inventories** – Filter your shop by items on another user's wantlist – or the other way around.
  Works with collections too.
- **Group by seller** – See which sellers have the most listings matching a wantlist, including listing details.
- **Caching & configuration** – Results are cached locally. Username and API token can be stored persistently.

## Demo

![discogs-ctl Demo](docs/demo.gif)

## Installation

Install `discogs-ctl` using one of the following methods:

**Build from source:**

Before getting started with discogs-ctl, ensure your runtime environment meets the following requirements:

- **Programming Language:** Kotlin
- **Build Tool:** Gradle (wrapper included)

1. Clone the discogs-ctl repository:
    ```sh
    git clone https://github.com/ezienecker/discogs-ctl
    ```
2. Navigate to the project directory:
    ```sh
    cd discogs-ctl
    ```
3. Build the project:
    ```sh
    ./gradlew build
    ```

## Usage

To run the project after building it, you can add an alias to your shell configuration file (e.g., `.bashrc`, `.zshrc`):

```sh
alias discogs-ctl="java -jar /path/to/discogs-ctl/build/libs/discogs-ctl-1.0.0-SNAPSHOT.jar"
```

With that set up, you can use the `discogs-ctl` command from anywhere in your terminal.

```sh
discogs-ctl wantlist --username Madlip89
```

**Configure defaults:**

To configure the default username, run the following command:

```sh
discogs-ctl config set username Madlip89
```

By default, all requests are unauthenticated.
If you want to make authenticated requests, you can set a token via the config command.
This is then sent in the authorization header.

In order to generate a token, you need to create a personal access token on the Discogs website.
Go to your [Developer Settings](https://www.discogs.com/settings/developers) or go
to [discogs.com](https://www.discogs.com) click your user avatar on the top right of your screen, then "Settings",
then "Developers" and click "Generate new token".

```sh
discogs-ctl config set token <personal_access_token>
```

**Commands**

For a full list of available commands, run the following command:

```sh
discogs-ctl --help
```

## Support

If you found a bug or have a feature request, please open an [issue](https://github.com/ezienecker/discogs-ctl/issues)
