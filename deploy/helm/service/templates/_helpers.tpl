{{- define "service.name" -}}
{{- required "name e obrigatorio" .Values.name -}}
{{- end -}}

{{- define "service.labels" -}}
app.kubernetes.io/name: {{ include "service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: digitalbanking
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "service.requiredEnv" -}}
{{- range .Values.requiredEnv }}
{{- if not (get $.Values.env . | toString | trim) }}
{{- fail (printf "env.%s e obrigatorio e nao pode ficar vazio (informe com --set-string env.%s=... ou -f)" . .) }}
{{- end }}
{{- end }}
{{- end -}}
