###########################################################################
<solution territory='${name}'>
	<trains>
<#list trains as train> 
		<train id='${train.name}'>
			<movements>
<#list train.movements as movement> 
				<movement arc='(${movement.origin},${movement.destination})' entry='${movement.entry}' exit='${movement.exit}' />
</#list>
<#if train.destinationEntry??>
				<destination entry='${train.destinationEntry}' />
</#if>
			</movements>
		</train>
</#list> 
	</trains>
</solution>
###########################################################################
