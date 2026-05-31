import { Marked } from 'marked'
import hljs from 'highlight.js'

const marked = new Marked({
  breaks: true,
  gfm: true,
  renderer: {
    code({ text, lang }: { text: string; lang?: string }) {
      const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
      const highlighted = hljs.highlight(text, { language }).value
      return `<pre class="code-block"><div class="code-header"><span class="code-lang">${language}</span><button class="code-copy" onclick="navigator.clipboard.writeText(this.closest('pre').querySelector('code').innerText)">复制</button></div><code class="hljs language-${language}">${highlighted}</code></pre>`
    },
  },
})

export function renderMarkdown(content: string): string {
  if (!content) return ''
  return marked.parse(content) as string
}
