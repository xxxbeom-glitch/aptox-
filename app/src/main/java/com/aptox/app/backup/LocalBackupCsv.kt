package com.aptox.app.backup

/**
 * 최소 CSV 이스케이프 (쉼표·따옴표·개행 포함 필드 지원).
 */
object LocalBackupCsv {

    fun parseHeaderLine(line: String): List<String> =
        parseLine(line).map { it.trim() }

    fun headersMatch(actual: List<String>, expected: List<String>): Boolean =
        actual.size == expected.size && actual.zip(expected).all { (a, e) -> a == e }

    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        val sb = StringBuilder()
        while (i < line.length) {
            when (val c = line[i]) {
                '"' -> {
                    i++
                    while (i < line.length) {
                        when (line[i]) {
                            '"' -> {
                                if (i + 1 < line.length && line[i + 1] == '"') {
                                    sb.append('"')
                                    i += 2
                                } else {
                                    i++
                                    break
                                }
                            }
                            else -> {
                                sb.append(line[i])
                                i++
                            }
                        }
                    }
                }
                ',' -> {
                    result.add(sb.toString())
                    sb.clear()
                    i++
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        result.add(sb.toString())
        return result
    }

    fun escapeField(raw: String): String {
        if (raw.none { it == ',' || it == '"' || it == '\n' || it == '\r' }) return raw
        return '"' + raw.replace("\"", "\"\"") + '"'
    }

    fun formatRow(fields: List<String>): String = fields.joinToString(",") { escapeField(it) }
}
