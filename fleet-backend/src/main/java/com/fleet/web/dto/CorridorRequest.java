package com.fleet.web.dto;

/** Per-trip route corridor configuration: permitted deviation distance and confirmations. */
public record CorridorRequest(Integer deviationThresholdM, Integer deviationConfirmations) {
}
