## What is this?

A very specific but handy tool to query GitHub commits from an organization 
that were created the last couple of days. Useful when you have to collect your team's commits before presenting a demo.

## How to run this?

You will need to create a personal access token for your GitHub user with repo scope.
For details, see: https://docs.github.com/en/rest/guides/getting-started-with-the-rest-api#using-personal-access-tokens

After that, fill in the environment variables and run:

<pre>
docker pull pvalyi/github-query-tool:latest && docker run --rm \
    -e GITHUB_OAUTH='your github token' \
    -e ORG='your organization' \
    -e REPO_REGEX='regex for repo name filtering' \
    -e LOOKBACK_DAYS='max days to look back in commits (default 7)' \
    pvalyi/github-query-tool
</pre>

Enjoy!


