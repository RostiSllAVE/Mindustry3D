"""Read-only runtime resolution/safety regressions; fake packages remain inside ignored local evidence."""
import json,os,subprocess,sys,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch
import dev_runtime as dev

class RuntimeTest(unittest.TestCase):
    def test_installed_version(self):
        info=dev.inspect();self.assertEqual(info['gameVersion'],'160.7');self.assertNotEqual(info['modifier'],'steam');self.assertTrue(info['javaVersion'].startswith('25.'))
    def test_java17(self):self.assertTrue(dev.runtime('java17').is_file())
    def test_jar_path(self):self.assertEqual(dev.host_jar(dev.host_jar()),dev.host_jar())
    def test_missing_does_not_fallback(self):
        with self.assertRaisesRegex(FileNotFoundError,'No Steam fallback'):dev.host_jar(dev.ROOT/'local/standalone-setup/not-a-game')
    def test_wrong_version_and_steam_rejected(self):
        folder=Path(tempfile.mkdtemp(prefix='fake-sdk-',dir=dev.ROOT/'local/standalone-setup'));(folder/'jre').mkdir()
        for version,modifier in [('160.5','release'),('160.7','steam')]:
            with zipfile.ZipFile(folder/'jre/desktop.jar','w') as z:z.writestr('version.properties','build='+version+'\nmodifier='+modifier+'\n')
            with self.assertRaises(ValueError):dev.inspect(folder)
    def test_launcher_syntax(self):
        compile((dev.ROOT/'tools/launch-standalone.py').read_text(),'launch-standalone.py','exec')
        compile((dev.ROOT/'tools/build-diagnostics.py').read_text(),'build-diagnostics.py','exec')
        compile((dev.ROOT/'tools/build.py').read_text(),'build.py','exec')

if __name__=='__main__':unittest.main()
