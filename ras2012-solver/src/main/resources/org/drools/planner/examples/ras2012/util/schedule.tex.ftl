\begin{sidewaystable}
\footnotesize
\caption{Resolved system ``${name}'', costing \$${cost}. Seed: ${seed}.}
\centering
\begin{tabular}{c||c|c|c||c|c|c|c||c|c|c}
  \hline \hline
  &
  Unpref. & 
  Delay &
  Pty &
  Node &
  SA &
  Actual &
  Pty &
  TWT &
  Actual &
  Pty \\
  <#list trains as train> 
    <#assign num = train.numStops>
    <#assign i = 1>
    <#list train.stops as stop>
      <#if i == 1>\hline<#else>\cline{5-8}</#if>
      <#if i == 1>\multirow{${num}}{*}{${train.name}}</#if> &
      <#if i == 1>\multirow{${num}}{*}{${train.unpreferredPenalty}}</#if> &
      <#if i == 1>\multirow{${num}}{*}{${train.delay}}</#if> &
      <#if i == 1>\multirow{${num}}{*}{${train.delayPenalty}}</#if> &
      ${stop.node} &
      ${stop.sa} &
      <#if stop.saDiff??>
        ${stop.arrive} &
        ${stop.saPenalty} &
      <#else>
        \multicolumn{2}{|c||}{N/A} &
      </#if>
      <#if i == 1>\multirow{${num}}{*}{${train.twt}} &</#if>
      <#if train.twtDiff??>
        <#if i == 1>\multirow{${num}}{*}{${train.twtArrive}}</#if> &
        <#if i == 1>\multirow{${num}}{*}{${train.twtPenalty}}</#if>
      <#else>
        <#if i == 1>\multicolumn{2}{c}{\multirow{${num}}{*}{N/A}}</#if>
      </#if>
      \\
      <#assign i = i + 1>
    </#list> 
  </#list> 
\end{tabular}
\label{table:${id}} 
\end{sidewaystable}