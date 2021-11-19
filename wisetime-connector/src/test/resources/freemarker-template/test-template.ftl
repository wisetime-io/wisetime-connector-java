${getGroupName()} [${getTotalDurationSecs()?string.@duration}]
<#if getDescription()?has_content>${getDescription()}</#if>

<#list getTimeRows() as timeRow>
  ${timeRow.getSubmittedDate()?string.@printSubmittedDate_HH\:mm} - ${timeRow.getActivity()} - ${timeRow.getDescription()}
</#list>