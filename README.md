<div style="max-width: 900px; margin: 2rem auto; font-family: -apple-system, BlinkMacOSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333;">

<h1 style="color: #1a1a1a; border-bottom: 2px solid #ddd; padding-bottom: 0.4em;">
  Commit Message Formats
</h1>

<h2>Types</h2>

<p><strong>API relevant changes</strong></p>

<ul style="list-style: none; padding-left: 1.5rem;">
  <li><strong>feat</strong> — Commits, that adds or remove a new feature</li>
  <li><strong>fix</strong> — Commits, that fixes a bug</li>
</ul>

<p><strong>refactor</strong> — Commits, that rewrite/restructure your code, however does not change any API behaviour</p>
<p><strong>perf</strong> — Commits are special refactor commits, that improve performance</p>

<ul style="list-style: none; padding-left: 1.5rem;">
  <li><strong>style</strong> — Commits, that do not affect the meaning (white-space, formatting, missing semi-colons, etc)</li>
  <li><strong>test</strong> — Commits, that add missing tests or correcting existing tests</li>
  <li><strong>docs</strong> — Commits, that affect documentation only</li>
  <li><strong>build</strong> — Commits, that affect build components like build tool, ci pipeline, dependencies, project version, ...</li>
  <li><strong>ops</strong> — Commits, that affect operational components like infrastructure, deployment, backup, recovery, ...</li>
  <li><strong>chore</strong> — Miscellaneous commits e.g. modifying .gitignore</li>
</ul>

<h2>Scopes</h2>
<p>The scope provides additional contextual information.</p>
<ul style="list-style: none; padding-left: 1.5rem;">
  <li>Is an optional part of the format</li>
  <li>Allowed Scopes depends on the specific project</li>
  <li>Don't use issue identifiers as scopes</li>
</ul>

<h2>Breaking Changes Indicator</h2>
<p>Breaking changes should be indicated by an <code>!</code> before the <code>:</code> in the subject line e.g. <code>feat(api)!: remove status endpoint</code></p>
<ul style="list-style: none; padding-left: 1.5rem;">
  <li>Is an optional part of the format</li>
</ul>

<h2>Description</h2>
<p>The description contains a concise description of the change.</p>
<ul style="list-style: none; padding-left: 1.5rem;">
  <li>Is a mandatory part of the format</li>
  <li>Use the imperative, present tense: "change" not "changed" nor "changes"</li>
  <li>Think of <em>This commit will...</em> or <em>This commit should...</em></li>
  <li>Don't capitalize the first letter</li>
  <li>No dot (.) at the end</li>
</ul>

<h2>Body</h2>
<p>The body should include the motivation for the change and contrast this with previous behavior.</p>
<ul style="list-style: none; padding-left: 1.5rem;">
  <li>Is an optional part of the format</li>
  <li>Use the imperative, present tense: "change" not "changed" nor "changes"</li>
  <li>This is the place to mention issue identifiers and their relations</li>
</ul>

<h2>Footer</h2>
<p>The footer should contain any information about Breaking Changes and is also the place to reference Issues that this commit refers to.</p>
<ul style="list-style: none; padding-left: 1.5rem;">
  <li>Is an optional part of the format</li>
  <li>optionally reference an issue by its id.</li>
  <li>Breaking Changes should start with the word <strong>BREAKING CHANGES:</strong> followed by space or two newlines. The rest of the commit message is then used for this.</li>
</ul>

<h3>Initial Commit</h3>
<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; overflow-x: auto; font-family: 'SF Mono', Monaco, Consolas, monospace; font-size: 0.95em;">
chore: init
</pre>

<h3>Examples</h3>
<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace; white-space: pre-wrap;">
feat: add email notifications on new direct messages
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
feat(shopping cart): add the amazing button
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
feat!: remove ticket list endpoint

refers to JIRA-1337

BREAKING CHANGES: ticket endpoints no longer supports list all entites.
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
fix(api): handle empty message in request body
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
fix(api): fix wrong calculation of request body checksum
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
fix(*): add missing parameter to service call
- The error occurred because of <reasons>.
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
perf: decrease memory footprint for determine uniqe visitors by using HyperLogLog
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
build: update dependencies
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
build(release): bump version to 1.0.0
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
refactor: implement fibonacci number calculation as recursion
</pre>

<pre style="background: #f6f8fa; padding: 1rem; border-radius: 6px; margin: 0.8rem 0; font-family: 'SF Mono', Monaco, Consolas, monospace;">
style: remove empty line
</pre>

</div>
