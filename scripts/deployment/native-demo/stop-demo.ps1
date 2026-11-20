[CmdletBinding()]
param()
# There is deliberately no process-name based kill or Windows service stop.
# The preparation-only start script cannot create a managed runtime.
Write-Host 'No native-demo runtime has been implemented or started by this kit. No process stopped.'
Write-Host 'Do not stop java, node, mysqld or Windows services by name: they may belong to another project.'
