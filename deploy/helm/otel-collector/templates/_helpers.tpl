{{- define "collector.labels" -}}
app.kubernetes.io/name: {{ .Values.name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: digitalbanking
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "collector.selectorLabels" -}}
app.kubernetes.io/name: {{ .Values.name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
