"""First-launch and Gemini instruction fixtures, without real agent environments."""
from concurrent.futures import ThreadPoolExecutor
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
HELPER = ROOT / 'app/src/main/assets/agent-instructions.py'
SPEC = importlib.util.spec_from_file_location('instructions', HELPER)
instructions = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(instructions)


class FirstLaunch(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir=ROOT / '.verification')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.home = self.root / 'home'
        self.home.mkdir()
        self.project = self.root / 'project'
        self.project.mkdir()
        self.template = self.root / 'template.md'
        self.template.write_bytes(b'First-launch fixture\n')

    def editors(self):
        return [editor(self.template, self.home, {}) for editor in
                (instructions.Editor, instructions.ClaudeEditor, instructions.GeminiEditor)]

    def gemini(self, runtime='antigravity', **env):
        return instructions.GeminiEditor(self.template, self.home, env, runtime)

    def project_request(self, name='GEMINI.md'):
        return {'scope': 'project', 'project': str(self.project), 'name': name}

    def test_only_common_file_initialized_once_and_empty_files_preserved(self):
        for editor in self.editors():
            with self.subTest(agent=editor.agent):
                path = editor.location({})
                self.assertFalse(path.parent.exists())
                init = dict(self.project_request('LEARNING.md'), operation='init')
                self.assertEqual({'created': True, 'path': str(path)}, editor.call(init))
                self.assertEqual(self.template.read_bytes(), path.read_bytes())
                path.unlink()
                self.assertFalse(editor.initialize()['created'])
                self.assertFalse(path.exists())
                self.assertFalse(any(self.project.iterdir()))

    def test_skips_user_files_even_empty_and_remembers_skip(self):
        for editor in self.editors():
            with self.subTest(agent=editor.agent):
                target = editor.location({})
                target.parent.mkdir()
                existing = target.with_name('AGENTS.override.md') if editor.agent == 'codex' else target
                existing.touch()
                self.assertFalse(editor.initialize()['created'])
                self.assertEqual(b'', existing.read_bytes())
                existing.unlink()
                self.assertFalse(editor.initialize()['created'])
                self.assertFalse(target.exists())

    def test_runtime_home_override_and_first_launch_are_scoped(self):
        legacy_home = str(self.root / 'legacy-home')
        legacy = self.gemini('legacy', GEMINI_CLI_HOME=legacy_home)
        antigravity = self.gemini(GEMINI_CLI_HOME=legacy_home)
        self.assertEqual(str(Path(legacy_home) / '.gemini/GEMINI.md'), legacy.view({})['path'])
        self.assertTrue(legacy.view({})['homeExplicit'])
        self.assertEqual(str(self.home / '.gemini/GEMINI.md'), antigravity.view({})['path'])
        self.assertFalse(antigravity.view({})['homeExplicit'])
        self.assertTrue(legacy.initialize()['created'])
        self.assertTrue(antigravity.initialize()['created'])
        antigravity.location({}).unlink()
        self.assertFalse(self.gemini('legacy').initialize()['created'])

    def test_existing_antigravity_global_rule_prevents_template(self):
        for index, name in enumerate(('AGENTS.md', 'config/AGENTS.md',
                                      'config/GEMINI.md', 'config/rules/custom.md')):
            with self.subTest(name=name):
                editor = instructions.GeminiEditor(self.template, self.root / str(index), {})
                existing = editor.gemini_home / name
                existing.parent.mkdir(parents=True)
                existing.write_bytes(b'Custom global rules\n')
                self.assertFalse(editor.initialize()['created'])
                self.assertFalse(editor.location({}).exists())
                self.assertEqual(b'Custom global rules\n', existing.read_bytes())
                existing.unlink()
                self.assertFalse(editor.initialize()['created'])

    def test_recovers_from_failed_publish_and_serializes_concurrent_launches(self):
        editor = self.gemini()
        with patch.object(instructions, 'publish_new', side_effect=OSError('fixture failure')):
            with self.assertRaises(OSError):
                editor.initialize()
        self.assertFalse(editor.location({}).exists())

        def launch(_):
            return self.gemini().initialize()['created']

        with ThreadPoolExecutor(max_workers=2) as pool:
            self.assertEqual([False, True], sorted(pool.map(launch, range(2))))
        self.assertEqual(self.template.read_bytes(), editor.location({}).read_bytes())

    def test_does_not_replace_a_file_created_during_publication(self):
        editor = self.gemini()
        publish = instructions.publish_new

        def external_write(source, target):
            target.write_text('external writer')
            publish(source, target)

        with patch.object(instructions, 'publish_new', side_effect=external_write):
            self.assertFalse(editor.initialize()['created'])
        self.assertEqual('external writer', editor.location({}).read_text())

    def test_initialization_marker_never_follows_symlinks(self):
        editor = self.gemini()
        target = self.root / 'unrelated.md'
        target.write_bytes(b'1\n')
        (editor.storage() / 'gemini-initialized').symlink_to(target)
        with self.assertRaisesRegex(instructions.Refused, 'UNSAFE_OR_UNREADABLE_FILE'):
            editor.initialize()
        self.assertFalse(editor.location({}).exists())
        self.assertEqual(b'1\n', target.read_bytes())

    def test_recovers_from_failed_marker_without_replacing_published_template(self):
        editor = self.gemini()
        with patch.object(instructions, 'atomic', side_effect=OSError('fixture failure')):
            with self.assertRaises(OSError):
                editor.initialize()
        path = editor.location({})
        self.assertEqual(self.template.read_bytes(), path.read_bytes())
        path.write_text('edited after startup')
        self.assertFalse(editor.initialize()['created'])
        self.assertEqual('edited after startup', path.read_text())
        path.unlink()
        self.assertFalse(editor.initialize()['created'])

    def test_gemini_project_save_history_conflict_and_shared_memory(self):
        editor = self.gemini()
        request = self.project_request()
        first = editor.view(request)
        self.assertFalse(first['exists'])
        self.assertFalse(first['projectBaseExists'])
        self.assertFalse(editor.gemini_home.exists())

        memory = self.root / 'LEARNING.md'
        memory.write_text('shared memory')
        (self.project / 'AGENTS.md').write_text('Keep notes in `../LEARNING.md`.')
        rules = '@../LEARNING.md\nGemini rules'
        current = editor.save(dict(request, path=first['path'], revision=first['revision'], text=rules))
        self.assertTrue(current['projectBaseExists'])
        self.assertEqual(str(memory), editor.view(self.project_request('LEARNING.md'))['path'])
        self.assertFalse(editor.location({}).exists())
        with self.assertRaisesRegex(instructions.Refused, 'EDIT_CONFLICT'):
            editor.save(dict(request, path=first['path'], revision=first['revision'], text='stale'))
        editor.save(dict(request, path=current['path'], revision=current['revision'], text='next'))
        previous = editor.history(request)['history'][0]['id']
        self.assertEqual(rules, editor.previous(dict(request, id=previous))['text'])
        with self.assertRaisesRegex(instructions.Refused, 'INVALID_SCOPE'):
            editor.view(self.project_request('AGENTS.md'))

    def test_shared_memory_reference_cannot_follow_a_symlink_to_another_file(self):
        (self.project / '.git').mkdir()
        sentinel = self.root / 'private-fixture.json'
        sentinel.write_text('private fixture')
        (self.root / 'LEARNING.md').symlink_to(sentinel)
        for editor, name in ((self.gemini(), 'GEMINI.md'),
                             (instructions.ClaudeEditor(self.template, self.home, {}), 'CLAUDE.md')):
            with self.subTest(agent=editor.agent):
                (self.project / name).write_text('@../LEARNING.md')
                with self.assertRaisesRegex(instructions.Refused, 'UNSAFE_OR_UNREADABLE_FILE'):
                    editor.view(self.project_request('LEARNING.md'))
        self.assertEqual('private fixture', sentinel.read_text())

    def test_diagnostics_inventory_excludes_settings_and_does_not_assert_active_rules(self):
        editor = self.gemini()
        legacy_editor = self.gemini('legacy')
        editor.initialize()
        (self.project / '.git').mkdir()
        sub = self.project / 'sub'
        sub.mkdir()
        for path in (self.project / 'AGENTS.md', sub / 'GEMINI.md'):
            path.write_text('contents stay private')
        for name in ('settings.json', 'oauth_creds.json'):
            (editor.gemini_home / name).write_text('fixture key')
        read_file = instructions.read_file

        def verify_read(path, *args):
            self.assertIn(path.name, ('GEMINI.md', 'AGENTS.md'))
            return read_file(path, *args)

        with patch.object(instructions, 'read_file', side_effect=verify_read):
            result = editor.diagnostics({'project': str(sub)})
            legacy = legacy_editor.diagnostics({'project': str(sub)})
        self.assertEqual(['GEMINI.md', 'AGENTS.md', 'GEMINI.md'],
                         [Path(source['path']).name for source in result['sources']])
        self.assertEqual(['GEMINI.md', 'GEMINI.md'],
                         [Path(source['path']).name for source in legacy['sources']])
        self.assertFalse(result['cli']['behaviorVerified'])
        self.assertNotIn('fixture key', json.dumps(result))
        self.assertNotIn('contents stay private', json.dumps(result))

    def test_helper_json_routes_gemini_and_rejects_skills_and_unknown_runtime(self):
        env = {'HOME': str(self.home), 'PATH': os.environ.get('PATH', '')}

        def call(**fields):
            done = subprocess.run([sys.executable, '-I', str(HELPER), str(self.template)],
                                  input=json.dumps(dict(agent='gemini', **fields)), text=True,
                                  env=env, check=False, capture_output=True)
            return done.returncode, json.loads(done.stdout)

        status, reply = call(geminiRuntime='legacy', operation='init')
        self.assertEqual(0, status)
        self.assertTrue(reply['result']['created'])
        self.assertEqual((1, {'ok': False, 'error': 'INVALID_GEMINI_RUNTIME'}),
                         call(geminiRuntime='unknown'))
        for options in ({'geminiRuntime': 'legacy'}, {'environment': 'ubuntu'}):
            self.assertEqual((1, {'ok': False, 'error': 'INVALID_AGENT'}), call(kind='skills', **options))


if __name__ == '__main__':
    unittest.main()
