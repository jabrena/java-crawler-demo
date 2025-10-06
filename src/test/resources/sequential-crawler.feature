Feature: Web Crawler
  As a user of the web crawler
  I want to crawl websites
  So that I can collect and analyze web page information

  Background:
    Given a crawler is initialized with default settings

  Scenario: Crawl a single page successfully
    Given a crawler with max pages set to 1
    When I start crawling from a valid seed URL
    Then the crawler should fetch exactly 1 page
    And the crawl result should contain the page title
    And the crawl result should contain the page content
    And the crawl result should be marked as complete

  Scenario: Crawl multiple pages following links
    Given a crawler with max pages set to 10
    And a crawler with max depth set to 2
    When I start crawling from a page with multiple links
    Then the crawler should fetch multiple pages
    And the crawler should follow links up to depth 2
    And all crawled pages should be in the result

  Scenario: Respect maximum depth limit
    Given a crawler with max depth set to 1
    When I start crawling from a page with nested links
    Then the crawler should only crawl to depth 1
    And links at depth 2 should not be crawled

  Scenario: Respect maximum pages limit
    Given a crawler with max pages set to 3
    When I start crawling from a page with many links
    Then the crawler should stop after crawling 3 pages
    And the remaining links should not be processed

  Scenario: Handle duplicate URLs correctly
    Given a crawler configured to avoid duplicates
    When I crawl a page that links to the same URL multiple times
    Then each unique URL should only be visited once
    And duplicate URLs should be filtered out

  Scenario: Normalize URLs with fragments
    Given a crawler that normalizes URLs
    When I encounter URLs with different fragments pointing to the same page
    Then the crawler should treat them as the same URL
    And should only visit the URL once

  Scenario: Normalize URLs with trailing slashes
    Given a crawler that normalizes URLs
    When I encounter URLs with and without trailing slashes
    Then the crawler should treat them as the same URL
    And should only visit the URL once

  Scenario: Handle failed HTTP requests gracefully
    Given a crawler with max pages set to 5
    When I start crawling and encounter a 404 error
    Then the crawler should record the failed URL
    And the crawler should continue crawling other URLs
    And the crawl result should contain failed URLs list

  Scenario: Handle connection timeouts
    Given a crawler with timeout set to 1000 milliseconds
    When I crawl a URL that takes longer to respond
    Then the crawler should timeout after 1000 milliseconds
    And the crawler should record the URL as failed
    And the crawler should continue with remaining URLs

  Scenario: Follow only same-domain links by default
    Given a crawler with follow external links set to false
    And a start domain is configured
    When I crawl a page containing both internal and external links
    Then the crawler should only follow internal links
    And external links should be filtered out

  Scenario: Follow external links when configured
    Given a crawler with follow external links set to true
    When I crawl a page containing both internal and external links
    Then the crawler should follow both internal and external links
    And all valid links should be added to the queue

  Scenario: Extract all valid HTTP links from a page
    Given a crawler is processing a web page
    When the page contains various types of links
    Then the crawler should extract links starting with http://
    And the crawler should extract links starting with https://
    And the crawler should ignore empty links
    And the crawler should convert relative links to absolute URLs

  Scenario: Use breadth-first traversal
    Given a crawler with sufficient depth and page limits
    When I crawl a page with a tree structure of links
    Then pages at depth 0 should be crawled first
    And pages at depth 1 should be crawled next
    And pages at depth 2 should be crawled last
    And the traversal order should follow breadth-first pattern

  Scenario: Crawl result contains statistics
    Given a crawler with max pages set to 5
    When I complete a crawl session
    Then the result should contain total pages crawled
    And the result should contain successful pages list
    And the result should contain failed URLs list
    And the result should be marked as complete

  Scenario: Empty crawl result initialization
    When a crawl session starts
    Then the initial result should have zero pages crawled
    And the initial result should have empty successful pages list
    And the initial result should have empty failed URLs list
    And the initial result should not be marked as complete

  Scenario: Immutable result updates with successful pages
    Given an empty crawl result
    When a page is successfully crawled
    Then a new result instance should be created
    And the new result should contain the successful page
    And the original result should remain unchanged

  Scenario: Immutable result updates with failed URLs
    Given an empty crawl result
    When a URL fails to be crawled
    Then a new result instance should be created
    And the new result should contain the failed URL
    And the original result should remain unchanged

