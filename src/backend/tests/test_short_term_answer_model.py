from __future__ import annotations

import os
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from online_query import query_engine
from em2mem.llm import LLMModel
from em2mem.llm import openai_gpt


class ShortTermAnswerModelTests(unittest.TestCase):
    def setUp(self) -> None:
        query_engine._SHORT_TERM_ANSWER_MODELS.clear()

    def tearDown(self) -> None:
        query_engine._SHORT_TERM_ANSWER_MODELS.clear()

    def test_remote_profile_uses_its_endpoint_without_loading_memory(self) -> None:
        constructor = Mock()
        module = types.ModuleType("em2mem.llm")
        module.LLMModel = constructor
        env = {
            "EM2MEM_QUERY_RESPOND_MODEL": "hermes-agent",
            "OPENAI_BASE_URL": "http://127.0.0.1:8642/v1",
            "OPENAI_API_KEY": "test-remote-key",
        }
        with patch.dict(os.environ, env, clear=True), patch.dict(sys.modules, {"em2mem.llm": module}), patch.object(
            query_engine, "_em2mem_classes", side_effect=AssertionError("Long-term stack was loaded")
        ):
            first = query_engine._get_short_term_answer_model()
            self.assertIs(first, query_engine._get_short_term_answer_model())
        constructor.assert_called_once_with(
            model_name="hermes-agent", provider="openai", fps=1, max_retries=3,
            api_key="test-remote-key", base_url="http://127.0.0.1:8642/v1", local_mode=False,
        )

    def test_local_profile_keeps_its_separate_endpoint(self) -> None:
        constructor = Mock()
        module = types.ModuleType("em2mem.llm")
        module.LLMModel = constructor
        env = {
            "EM2MEM_QUERY_RESPOND_MODEL": "Qwen3.5-9B",
            "EM2MEM_LOCAL_LLM_ENABLED": "1",
            "EM2MEM_LOCAL_LLM_BASE_URL": "http://127.0.0.1:18100/v1",
            "EM2MEM_LOCAL_LLM_API_KEY": "test-local-key",
            "OPENAI_BASE_URL": "https://remote.example/v1",
            "OPENAI_API_KEY": "test-remote-key",
        }
        with patch.dict(os.environ, env, clear=True), patch.dict(sys.modules, {"em2mem.llm": module}):
            query_engine._get_short_term_answer_model()
        self.assertEqual(constructor.call_args.kwargs["base_url"], "http://127.0.0.1:18100/v1")
        self.assertEqual(constructor.call_args.kwargs["api_key"], "test-local-key")
        self.assertTrue(constructor.call_args.kwargs["local_mode"])

    def test_hermes_model_is_passed_to_the_openai_client(self) -> None:
        with patch.object(openai_gpt, "OpenAI"), patch.object(openai_gpt, "AsyncOpenAI"):
            model = LLMModel(
                "hermes-agent", api_key="test-key", base_url="http://127.0.0.1:8642/v1", local_mode=False
            )
        self.assertEqual(model.provider, "openai")
        self.assertEqual(model.model.model_name, "hermes-agent")


if __name__ == "__main__":
    unittest.main()
