/**
 * Markdown 转 HTML
 */
export const markdownToHtml = (markdown: string): string => {
    return marked(markdown)
}

/** 转义 HTML 特殊字符，防止 XSS */
const escapeHtml = (text: string): string =>
  text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

/** 行内元素解析：行内代码、图片、链接、加粗、斜体 */
const parseInline = (text: string): string => {
  const escaped = escapeHtml(text)
  return escaped
    .replace(/`([^`]+)`/g, (_match: string, code: string) => `<code>${code}</code>`)
    .replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_match: string, alt: string, src: string) =>
      `<img src="${src}" alt="${alt}" />`)
    .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_match: string, label: string, href: string) =>
      `<a href="${href}" target="_blank" rel="noopener noreferrer">${label}</a>`)
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/_([^_]+)_/g, '<em>$1</em>')
}

/** Markdown 转 HTML */
const marked = (markdown: string): string => {
  const lines = markdown.replace(/\r\n/g, '\n').split('\n')
  const html: string[] = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i] ?? ''
    const trimmed = line.trim()

    // 空行跳过
    if (trimmed === '') {
      i++
      continue
    }

    // 分隔线
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      html.push('<hr />')
      i++
      continue
    }

    // 标题
    const heading = line.match(/^(#{1,6})\s+(.*)$/)
    if (heading) {
      const level = heading[1]!.length
      html.push(`<h${level}>${parseInline(heading[2] ?? '')}</h${level}>`)
      i++
      continue
    }

    // 代码块
    if (line.startsWith('```')) {
      const lang = (line.match(/^```(\w*)/) ?? [])[1] ?? ''
      const codeLines: string[] = []
      i++
      while (i < lines.length && !(lines[i] ?? '').startsWith('```')) {
        codeLines.push(lines[i] ?? '')
        i++
      }
      i++
      html.push(`<pre><code${lang ? ` class="language-${lang}"` : ''}>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
      continue
    }

    // 引用
    if (trimmed.startsWith('>')) {
      const quoteLines: string[] = []
      while (i < lines.length && (lines[i] ?? '').trim().startsWith('>')) {
        quoteLines.push((lines[i] ?? '').trim().replace(/^>\s?/, ''))
        i++
      }
      html.push(`<blockquote><p>${quoteLines.map(parseInline).join('<br />')}</p></blockquote>`)
      continue
    }

    // 无序列表
    if (/^\s*[-*+]\s+/.test(line)) {
      const items: string[] = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i] ?? '')) {
        items.push(parseInline((lines[i] ?? '').replace(/^\s*[-*+]\s+/, '')))
        i++
      }
      html.push(`<ul>${items.map((item) => `<li>${item}</li>`).join('')}</ul>`)
      continue
    }

    // 有序列表
    if (/^\s*\d+\.\s+/.test(line)) {
      const items: string[] = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i] ?? '')) {
        items.push(parseInline((lines[i] ?? '').replace(/^\s*\d+\.\s+/, '')))
        i++
      }
      html.push(`<ol>${items.map((item) => `<li>${item}</li>`).join('')}</ol>`)
      continue
    }

    // 段落（收集连续普通行，行内换行用 <br /> 连接）
    const paraLines: string[] = [line]
    i++
    while (i < lines.length) {
      const next = lines[i] ?? ''
      if (
        next.trim() === '' ||
        /^(#{1,6})\s/.test(next) ||
        next.startsWith('```') ||
        next.trim().startsWith('>') ||
        /^\s*([-*+]\s+|\d+\.\s+)/.test(next)
      ) {
        break
      }
      paraLines.push(next)
      i++
    }
    html.push(`<p>${paraLines.map(parseInline).join('<br />')}</p>`)
  }

  return html.join('\n')
}

