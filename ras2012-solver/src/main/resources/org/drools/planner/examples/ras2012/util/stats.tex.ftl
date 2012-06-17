\begin{table}
\footnotesize
\caption{Solution performance per data set.}
\centering
\begin{tabular}{c||c|c|c|c|c|c}
\hline \hline
                 & Best                 & Q1                 & Q2                & Q3                & Worst \\ 
<#list solutions as solution> 
\hline
${solution.name} & \$${solution.best}   & \$${solution.q1}   & \$${solution.q2}  & \$${solution.q3}  & \$${solution.worst} \\
</#list> 
\end{tabular} 
\label{table:result} 
\end{table}