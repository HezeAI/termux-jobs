import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import io

spec=importlib.util.spec_from_file_location('skills',Path(__file__).resolve().parents[2]/'app/src/main/assets/agent-skills.py')
s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)

class SkillsTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='skills-',dir=Path(__file__).resolve().parents[2]/'.verification')
        self.root=Path(self.temp.name);self.home=self.root/'home';self.home.mkdir();self.project=self.root/'project';self.project.mkdir();(self.project/'.git').mkdir()
        self.env=patch.dict(os.environ,{'HOME':str(self.home),'CODEX_HOME':str(self.home/'.codex'),'CLAUDE_CONFIG_DIR':str(self.home/'.claude')});self.env.start()
        self.source=self.root/'source';self.source.mkdir();self.skill(self.source,'sample','first')
        self.base={'agent':'codex','environment':'ubuntu-ai','project':str(self.project),'scope':'user'}
    def tearDown(self):self.env.stop();self.temp.cleanup()
    def skill(self,folder,name,body):
        folder.mkdir(parents=True,exist_ok=True);(folder/'SKILL.md').write_text(f'---\nname: {name}\ndescription: Harmless fixture\n---\n{body}\n')
    def call(self,op,**kw):return s.call(dict(self.base,operation=op,**kw))
    def prepare(self,path=None,kind='folder',**kw):return self.call('prepare',sourceKind=kind,source=str(path or self.source),**kw)
    def install(self,p=None,op='install',revision=''):
        p=p or self.prepare();item=p['candidates'][0];return self.call(op,token=p['token'],candidate=item['candidate'],revision=revision)
    def row(self):return next(x for x in self.call('list')['skills'] if x['name']=='sample')
    def test_install_all_agents_scopes(self):
        for agent in ('codex','claude'):
            for scope in ('user','project'):
                self.base.update(agent=agent,scope=scope);result=self.install();self.assertTrue(Path(result['path'],'SKILL.md').exists());self.assertEqual(result['discovery'],'unconfirmed');self.assertTrue(self.row()['managed'])
    def test_create_all_agents_scopes_and_literal_metadata(self):
        description='Use for: "quoted" tasks\nwith кириллица [data]'
        for agent in ('codex','claude'):
            for scope in ('user','project'):
                self.base.update(agent=agent,scope=scope)
                p=self.call('prepare',sourceKind='create',name='authored',description=description,instructions='# Instructions\nDo the agreed work.')
                self.assertEqual(p['source'],'created-in-app'); self.assertEqual(p['commit'],'')
                item=p['candidates'][0]; self.assertNotIn('error',item)
                self.assertEqual(item['description'],description)
                self.assertFalse(Path(item['path']).exists())
                result=self.install(p); self.assertEqual(result['discovery'],'unconfirmed')
                text=Path(result['path'],'SKILL.md').read_text()
                self.assertIn('# Instructions',text)
                with self.assertRaisesRegex(s.SkillError,'NAME_CONFLICT'): self.install(p)
    def test_create_invalid_fields_never_install(self):
        for fields,code in [({'name':'../bad'},'INVALID_NAME'),({'name':'Name With Space'},'INVALID_NAME'),({'name':'x'*65},'INVALID_NAME'),({'description':' '},'INVALID_DESCRIPTION'),({'description':'x'*1025},'INVALID_DESCRIPTION'),({'instructions':' '},'INSTRUCTIONS_REQUIRED')]:
            q=dict(sourceKind='create',name='good',description='When useful',instructions='Safe body');q.update(fields)
            with self.assertRaisesRegex(s.SkillError,code): self.call('prepare',**q)
        self.assertFalse((self.home/'.agents/skills/good').exists())
    def test_created_preview_still_binds_environment_root_and_source_hashes(self):
        p=self.call('prepare',sourceKind='create',name='authored',description='When useful',instructions='Safe body')
        self.base['scope']='project'
        with self.assertRaisesRegex(s.SkillError,'STALE_PREVIEW'):self.install(p)
        self.base['scope']='user'
        folder=Path(s.Manager(self.base).staging)/p['token']/p['candidates'][0]['candidate'];(folder/'SKILL.md').write_text('changed')
        with self.assertRaisesRegex(s.SkillError,'SOURCE_CHANGED'):self.install(p)
    def test_update_rollback_delete(self):
        first=self.install();row=self.row();self.skill(self.source,'sample','second');p=self.prepare();d=self.call('diff',token=p['token'],candidate=p['candidates'][0]['candidate']);self.assertIn('+second',d['instructionsDiff']);new=self.install(p,'update',row['revision']);self.assertNotEqual(first['revision'],new['revision']);self.assertTrue(self.row()['rollback']);back=self.call('rollback',name='sample',revision=new['revision']);self.assertEqual(first['revision'],back['revision']);self.call('delete',name='sample',revision=back['revision']);self.assertFalse(Path(first['path']).exists())
    def test_name_conflict(self):
        self.install()
        with self.assertRaisesRegex(s.SkillError,'NAME_CONFLICT'):self.install()
    def test_local_changes(self):
        r=self.install();Path(r['path'],'notes').write_text('local')
        with self.assertRaisesRegex(s.SkillError,'LOCAL_CHANGES'):self.install(op='update',revision=r['revision'])
        with self.assertRaisesRegex(s.SkillError,'LOCAL_CHANGES'):self.call('delete',name='sample',revision=r['revision'])
    def test_cancel_preview(self):
        p=self.prepare();self.call('discard',token=p['token']);self.assertFalse((self.home/'.agents/skills/sample').exists())
    def test_stale_scope(self):
        p=self.prepare();self.base['scope']='project'
        with self.assertRaisesRegex(s.SkillError,'STALE_PREVIEW'):self.install(p)
    def test_source_changed_after_preview(self):
        p=self.prepare();stage=Path(s.Manager(self.base).staging)/p['token']/p['candidates'][0]['candidate'];(stage/'SKILL.md').write_text('altered')
        with self.assertRaisesRegex(s.SkillError,'SOURCE_CHANGED'):self.install(p)
    def test_original_source_change_does_not_change_snapshot(self):
        p=self.prepare();self.skill(self.source,'sample','changed after preview');r=self.install(p);self.assertIn('first',Path(r['path'],'SKILL.md').read_text())
    def test_multiple(self):
        repo=self.root/'multi';self.skill(repo/'one','one','a');self.skill(repo/'two','two','b');p=self.prepare(repo);self.assertEqual(len(p['candidates']),2)
        for item in p['candidates']:self.call('install',token=p['token'],candidate=item['candidate'])
    def zip(self,items):
        p=self.root/'source.zip'
        with zipfile.ZipFile(p,'w') as z:
            for name,value in items:z.writestr(name,value)
        return p
    def test_zip(self):
        p=self.prepare(self.zip([('skill/SKILL.md',(self.source/'SKILL.md').read_bytes())]),'zip');self.install(p)
    def test_zip_traversal(self):
        with self.assertRaisesRegex(s.SkillError,'UNSAFE_ARCHIVE'):self.prepare(self.zip([('../escape',b'x')]),'zip')
        self.assertFalse((self.root/'escape').exists())
    def test_zip_symlink(self):
        item=zipfile.ZipInfo('link');item.external_attr=(0o120777<<16)
        with self.assertRaisesRegex(s.SkillError,'UNSAFE_ARCHIVE'):self.prepare(self.zip([(item,b'/tmp')]),'zip')
    def test_zip_corrupt(self):
        p=self.root/'bad.zip';p.write_bytes(b'bad')
        with self.assertRaisesRegex(s.SkillError,'INVALID_PACKAGE'):self.prepare(p,'zip')
    def test_zip_bomb(self):
        p=self.root/'bomb.zip'
        with zipfile.ZipFile(p,'w',compression=zipfile.ZIP_DEFLATED) as z:z.writestr('large',b'0'*(s.MAX_FILE+1))
        with self.assertRaisesRegex(s.SkillError,'SIZE_LIMIT'):self.prepare(p,'zip')
    def test_links(self):
        (self.source/'linked').symlink_to(self.root/'absent')
        with self.assertRaisesRegex(s.SkillError,'UNSAFE_LINK'):self.prepare()
    def test_hardlink(self):
        outside=self.root/'outside';outside.write_text('leave');os.link(outside,self.source/'hard')
        with self.assertRaisesRegex(s.SkillError,'UNSAFE_FILE'):self.prepare()
    def test_sensitive(self):
        (self.source/'auth.json').write_text('not read')
        with self.assertRaisesRegex(s.SkillError,'SENSITIVE_FILE'):self.prepare()
    def test_bad_metadata(self):
        (self.source/'SKILL.md').write_text('no frontmatter');p=self.prepare();self.assertEqual(p['candidates'][0]['error'],'INVALID_METADATA')
        with self.assertRaisesRegex(s.SkillError,'INVALID_PREVIEW'):self.install(p)
    def test_external_read_only(self):
        target=self.home/'.agents/skills/sample';self.skill(target,'sample','external')
        self.assertFalse(self.row()['managed'])
        with self.assertRaisesRegex(s.SkillError,'EXTERNAL_SKILL_READ_ONLY'):self.call('delete',name='sample',revision=self.row().get('revision','unowned'))
    def test_no_scripts_run(self):
        script=self.source/'scripts/run.sh';script.parent.mkdir();script.write_text('touch '+str(self.root/'unexpected'));script.chmod(0o755);r=self.install();self.assertFalse((self.root/'unexpected').exists());self.assertEqual(Path(r['path'],'scripts/run.sh').stat().st_mode&0o777,0o755)
    def test_github_pinned(self):
        stream=io.BytesIO()
        with zipfile.ZipFile(stream,'w') as z:z.writestr('repo-sha/sample/SKILL.md',(self.source/'SKILL.md').read_bytes())
        urls=[]
        def fake(url):
            urls.append(url)
            return (json.dumps({'sha':'a'*40}).encode() if '/commits/' in url else stream.getvalue())
        with patch.object(s,'fetch',fake):p=self.call('prepare',sourceKind='github',source='https://github.com/example/skills',ref='stable')
        self.assertEqual(p['commit'],'a'*40);self.assertIn('a'*40,urls[-1]);self.install(p)
    def test_private_or_other_url_rejected(self):
        for url in ('http://github.com/a/b','https://user:password@github.com/a/b','https://example.com/a/b'):
            with self.assertRaisesRegex(s.SkillError,'PUBLIC_GITHUB_ONLY'):self.call('prepare',sourceKind='github',source=url)
    def test_interruption_before_exchange(self):
        r=self.install();self.skill(self.source,'sample','next');p=self.prepare();original=s.atomic
        def interrupt(path,v):
            original(path,v)
            if path.name=='transaction.json':raise RuntimeError('fixture interruption')
        with patch.object(s,'atomic',interrupt):
            with self.assertRaises(s.SkillError):self.install(p,'update',r['revision'])
        self.assertIn('first',Path(r['path'],'SKILL.md').read_text());self.install(p,'update',r['revision']);self.assertIn('next',Path(r['path'],'SKILL.md').read_text())
    def test_interruption_after_exchange(self):
        r=self.install();self.skill(self.source,'sample','next');p=self.prepare();real=s.sync
        def interrupt(path):
            if path.name=='skills':raise RuntimeError('fixture interruption')
            return real(path)
        with patch.object(s,'sync',interrupt):
            with self.assertRaises(s.SkillError):self.install(p,'update',r['revision'])
        m=s.Manager(self.base);m.transaction(m.state/'sample');row=self.row();self.assertEqual(row['revision'],row['expected']);self.assertIn('next',Path(r['path'],'SKILL.md').read_text())
    def test_openai_dependency_declarations_are_previewed_not_run(self):
        config=self.source/'agents';config.mkdir();(config/'openai.yaml').write_text('dependencies:\n  tools:\n    - type: mcp\n      value: fixture\n')
        preview=self.prepare();self.assertIn('type: mcp',preview['candidates'][0]['declarations']);self.assertFalse((self.root/'unexpected').exists())

    def test_list_is_compact_and_inspect_reads_full_skill(self):
        result=self.install();row=self.row();self.assertNotIn('text',row)
        info=self.call('inspect',path=result['path']);self.assertIn('first',info['text']);self.assertIn('SKILL.md',info['files'])
    def test_inspect_outside_skill_roots_rejected(self):
        with self.assertRaisesRegex(s.SkillError,'INVALID_SOURCE'):self.call('inspect',path=str(self.source))
    def test_local_git_metadata_excluded(self):
        git=self.source/'.git';git.mkdir();(git/'auth.json').write_text('not imported')
        result=self.install();self.assertFalse(Path(result['path'],'.git').exists())

    def test_preview_limit_and_explicit_discard(self):
        prepared=[self.prepare() for _ in range(8)]
        with self.assertRaisesRegex(s.SkillError,'PREVIEW_LIMIT'):self.prepare()
        self.assertEqual(len(self.call('list')['pending']),8)
        self.call('discard',token=prepared[0]['token']);self.prepare()
    def test_explicit_recovery_after_new_install_interruption(self):
        preview=self.prepare();real=s.sync
        def interrupt(path):
            if path.name=='skills':raise RuntimeError('fixture interruption')
            return real(path)
        with patch.object(s,'sync',interrupt):
            with self.assertRaises(s.SkillError):self.install(preview)
        pending=self.call('list')['recovery'];self.assertEqual(pending[0]['name'],'sample')
        self.call('recover',name='sample');self.assertTrue(self.row()['managed']);self.assertEqual(self.call('list')['recovery'],[])
    def test_lock_symlink_not_followed(self):
        state=s.Manager(self.base).state;state.mkdir(parents=True)
        outside=self.root/'outside';outside.write_text('unchanged');(state/'lock').symlink_to(outside)
        with self.assertRaisesRegex(s.SkillError,'UNSAFE_LINK'):self.install()
        self.assertEqual(outside.read_text(),'unchanged')
    def test_atomic_rename_failure_preserves_current(self):
        result=self.install();self.skill(self.source,'sample','next');preview=self.prepare()
        with patch.object(s.ctypes,'CDLL',side_effect=RuntimeError('unsupported')):
            with self.assertRaises(s.SkillError):self.install(preview,'update',result['revision'])
        self.assertIn('first',Path(result['path'],'SKILL.md').read_text())
        self.call('recover',name='sample');self.assertEqual(self.row()['revision'],result['revision'])
    def test_local_corrupt_or_unsupported_metadata(self):
        for content in ('---\nname: sample\ndescription: true\n---\nBody','---\nname: sample\ndescription: !execute something\n---\nBody'):
            (self.source/'SKILL.md').write_text(content);preview=self.prepare();self.assertTrue(preview['candidates'][0].get('error'));self.call('discard',token=preview['token'])
    def test_external_legacy_and_plugin_not_owned(self):
        self.skill(self.home/'.codex/skills/legacy','legacy','legacy')
        self.skill(self.home/'.codex/plugins/cache/example/1/skills/plugin','plugin','plugin')
        rows=self.call('list')['skills'];self.assertTrue(any(x['name']=='legacy' and not x['managed'] for x in rows));self.assertTrue(any(x['name']=='plugin' and x['origin']=='plugin' and not x['managed'] for x in rows))
    def test_shared_scope_name_collisions(self):
        self.skill(self.home/'.agents/skills/sample','sample','personal')
        self.skill(self.project/'.agents/skills/sample','sample','project')
        self.assertEqual(len(self.row()['nameConflicts']),0)
        self.base['scope']='project'
        self.assertEqual(len(self.row()['nameConflicts']),1)

    def test_general_roots_and_rows_do_not_depend_on_project(self):
        for agent,folder in [('codex','.agents'),('claude','.claude')]:
            self.base.update(agent=agent,scope='user')
            self.skill(self.home/folder/'skills/personal','personal','shared')
            self.skill(self.project/folder/'skills/project_only','project-only','project')
            first=self.call('list')
            self.assertFalse(any(x['scope'] in ('project','ancestor') for x in first['roots']))
            self.assertTrue(any(x['path']==str(self.home/folder/'skills') for x in first['roots']))
            self.assertFalse(any(x['name']=='project-only' for x in first['skills']))
            self.base['project']=str(self.root/'another-project')
            second=self.call('list')
            self.assertEqual(first['roots'],second['roots'])
            self.assertEqual(first['skills'],second['skills'])
            self.assertEqual(first['target'],second['target'])
            self.base['project']=str(self.project)

    def test_general_listing_does_not_probe_project_git_tree(self):
        original=Path.exists
        def exists(path):
            if path.name=='.git' and self.project in path.parents:
                raise AssertionError('General list inspected project')
            return original(path)
        with patch.object(Path,'exists',exists):
            self.call('list')

    def test_delete_link_conflict_preserves_outside(self):
        r=self.install();target=Path(r['path']);outside=self.root/'outside';outside.mkdir();(outside/'keep').write_text('safe');(target/'link').symlink_to(outside)
        with self.assertRaisesRegex(s.SkillError,'UNSAFE_LINK'):self.call('delete',name='sample',revision=r['revision'])
        self.assertTrue((outside/'keep').exists())

if __name__=='__main__':unittest.main()
