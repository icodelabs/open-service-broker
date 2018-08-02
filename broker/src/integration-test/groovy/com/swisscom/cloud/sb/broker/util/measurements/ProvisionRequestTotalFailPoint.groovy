package com.swisscom.cloud.sb.broker.util.measurements

import org.influxdb.annotation.Column
import org.influxdb.annotation.Measurement

import java.time.Instant

@Measurement(name = "provisionRequest_total_fail")
class ProvisionRequestTotalFailPoint {
        @Column(name = "time")
        Instant time
        @Column(name = "value")
        double value
}
