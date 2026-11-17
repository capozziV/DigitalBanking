{{- define "db-init.labels" -}}
app.kubernetes.io/name: db-init
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: digitalbanking
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}
