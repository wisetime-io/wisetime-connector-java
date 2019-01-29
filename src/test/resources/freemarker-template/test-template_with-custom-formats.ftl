<#assign dateIsoUtc = "1990-06-04T09:00:00Z">
<#assign number = 64>
${getGroupName()?datetime.iso?string.@customDateFormat}
${getTotalDurationSecs()?string.@duration}