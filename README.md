# WiseTime Java Connector
[![Javadocs](https://www.javadoc.io/badge/io.wisetime/wisetime-connector.svg)](https://www.javadoc.io/doc/io.wisetime/wisetime-connector)

WiseTime Java Connector is an open source library that enables you to write a WiseTime connector with a few lines of Java code. It calls the [WiseTime Connect API](https://wisetime.com/docs/connect/api/) to communicate with WiseTime.

[WiseTime](https://wisetime.com) is a passive, privacy-first timekeeping system that summarises your tasks while you work. With [WiseTime Connect](https://wisetime.com/docs/connect/), you can connect your systems to WiseTime, so that you can automatically:

* Create tags and tag keywords in WiseTime when records are added to your system
* Receive posted time when users post their time sheets to WiseTime

## Getting Started

### Implement the WiseTimeConnector Interface

To create a connector, simply implement the `WiseTimeConnector` interface. Here's an example of a minimal implementation.

```java
public class HelloConnector implements WiseTimeConnector {

  private ConnectorModule cm;

  /**
   * Called by the WiseTime Connector library on connector initialization.
   */
  @Override
  public void init(final ConnectorModule connectorModule) {
    cm = connectorModule;
  }

  /**
   * Called by the WiseTime Connector library on a regular schedule.
   */
  @Override
  public void performTagUpdate() {
    // This is where you would query the connected system and send new tags to WiseTime.
    cm.apiClient.tagUpsert(
        new UpsertTagRequest()
            .name("Hello, World!")
            .path("/hello/connector/")
    );
  }

  /**
   * Called by the WiseTime Connector library whenever a user posts time to the team.
   */
  @Override
  public PostResult postTime(final Request request, final TimeGroup userPostedTime) {
    // This is where you would process the userPostedTime and create relevant
    // records in the connected system.
    return PostResult.SUCCESS;
  }
  
  /**
   * Identifies the type of the connector.
   */
  @Override
  public String getConnectorType() {
    // Custom implementation type 
    return "HELLO";
  }
}

```

### Start the Connector

Then, use your connector implementation when starting the connector.

```java
public class ConnectorLauncher {

  /**
   * Application entry point
   */
  public static void main(final String... args) throws Exception {
    
    Connector.newBuilder()
        .useFetchClient()
        .withWiseTimeConnector(new HelloConnector())
        .build()
        .start();
  }
}
```

The connector will launch a web server at port 8080 when calling useWebhook(). The server implements the [Posted Time Webhook](https://wisetime.com/docs/connect/posted-time-webhook/) that WiseTime will call whenever a user posts time to the team.

If you use `.useFetchClient()` instead, the connector will start in long polling mode to retrieve time posted to the team from WiseTime.

## Sample Project

Take a look at our open source [Jira Connector](https://github.com/wisetime-io/wisetime-jira-connector) for an example of a fully implemented, production-ready connector project. The connector is implemented in just [3 classes](https://github.com/wisetime-io/wisetime-jira-connector/tree/master/src/main/java/io/wisetime/connector/jira) and comes with great test coverage.

## Javadoc

The latest javadoc for the WiseTime Connector Library can be accessed [here](https://www.javadoc.io/doc/io.wisetime/wisetime-connector).
