\begin{table}
\caption{Statistics for resolved system ``${name}'', costing \$${cost}.}
\centering
\begin{tabular}{c||c|c||c|c|c|c|c||c|c|c}
  \hline \hline
  &
  Unpref. & 
  Delay &
  Node &
  When &
  SA &
  +/- &
  Pty &
  TWT &
  +/- &
  Pty \\
  <#list trains as train> 
    <#assign num = train.numStops>
    <#assign i = 1>
    <#list train.stops as stop>
      <#if i == 1>\hline<#else>\cline{4-8}</#if>
      <#if i == 1>\multirow{${num}}{*}{${train.name}}</#if> &
      <#if i == 1>\multirow{${num}}{*}{${train.unpreferredPenalty}}</#if> &
      <#if i == 1>\multirow{${num}}{*}{${train.delay}}</#if> &
      ${stop.node} &
      ${stop.arrive} &
      ${stop.sa} &
      <#if stop.saDiff??>
        ${stop.saDiff} &
        ${stop.saPenalty} &
      <#else>
        \multicolumn{2}{|c||}{Outside horizon} &
      </#if>
      <#if i == 1>\multirow{${num}}{*}{${train.twt}} &</#if>
      <#if train.twtDiff??>
        <#if i == 1>\multirow{${num}}{*}{${train.twtDiff}}</#if> &
        <#if i == 1>\multirow{${num}}{*}{${train.twtPenalty}}</#if>
      <#else>
        <#if i == 1>\multicolumn{2}{c}{\multirow{${num}}{*}{Outside horizon}}</#if>
      </#if>
      \\
      <#assign i = i + 1>
    </#list> 
  </#list> 
  \hline 
\end{tabular}
\label{table:${id}} 
\end{table}