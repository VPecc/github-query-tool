package com.pecc.gqt;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;

/**
 * * <li>GITHUB_LOGIN: username like 'kohsuke'
 * * <li>GITHUB_PASSWORD: raw password
 * * <li>GITHUB_OAUTH: OAuth token to login
 * * <li>GITHUB_ENDPOINT: URL of the API endpoint
 * * <li>GITHUB_JWT: JWT token to login
 */
public class Main {

    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final String ORG_ENV = "ORG";
    private static final String REPO_REGEX_ENV = "REPO_REGEX";
    private static final String LOOKBACK_DAYS_ENV = "LOOKBACK_DAYS";
    private static final long DEFAULT_LOOKBACK_DAYS = 7L;

    public static void main(String[] args) throws Exception {
        GitHub github = GitHubBuilder.fromEnvironment().build();
        String org = getOrg();
        String repoRegex = getRepoRegex();
        Instant committedSince = getCommittedSince();

        System.out.printf("searching in %s org in repositories %s for commits since %s%n",
                org,
                repoRegex,
                formatInstant(committedSince.truncatedTo(SECONDS)));

        StreamSupport.stream(github.searchRepositories()
                .org(org)
                .list()
                .spliterator(), false)
                .filter(repository -> repository.getFullName().matches(repoRegex))
                .peek(repository -> System.out.printf("%ncommits in %s%n", repository.getFullName()))
                .flatMap(repository -> getCommits(repository, committedSince))
                .forEachOrdered(Main::printCommit);
    }

    private static String getRepoRegex() {
        return Optional.ofNullable(System.getenv(REPO_REGEX_ENV))
                .orElseThrow(() -> new RuntimeException("Please set REPO_REGEX!"));
    }

    private static String getOrg() {
        return Optional.ofNullable(System.getenv(ORG_ENV))
                .orElseThrow(() -> new RuntimeException("Please set ORG!"));
    }

    private static Instant getCommittedSince() {
        long lookBackDays = Optional.ofNullable(System.getenv(LOOKBACK_DAYS_ENV))
                .map(Long::parseLong)
                .orElse(DEFAULT_LOOKBACK_DAYS);

        return Instant.now().minus(lookBackDays, DAYS);
    }

    private static Stream<GHCommit> getCommits(GHRepository repository, Instant commitedSince) {
            try {

                return repository.queryCommits()
                        .since(commitedSince.toEpochMilli())
                        .list()
                        .toSet()
                        .stream()
                        .sorted(comparing(Main::getCommitDate));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    private static void printCommit(GHCommit commit) {
        try {
            GHCommit.ShortInfo info = commit.getCommitShortInfo();
            String formattedDate = formatInstant(commit.getCommitDate().toInstant());

            System.out.printf(
                    "\t%s %s%n\t\t└ %s%s%s%n",
                    formattedDate,
                    info.getAuthor().getEmail(),
                    ANSI_YELLOW,
                    info.getMessage().replace(System.lineSeparator(), System.lineSeparator() + "\t\t  "),
                    ANSI_RESET);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatInstant(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private static Date getCommitDate(GHCommit ghCommit) {
        try {
            return ghCommit.getCommitDate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
