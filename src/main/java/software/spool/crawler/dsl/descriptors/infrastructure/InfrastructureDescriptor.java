package software.spool.crawler.dsl.descriptors.infrastructure;

public record InfrastructureDescriptor(
        WatchdogDescriptor watchdog,
        EventBusDescriptor eventBus,
        InboxDescriptor inbox,
        DataLakeDescriptor dataLake
) {
}
