#!/usr/bin/env python3
"""Parse exercise files and generate verb-to-be.json for the backend."""

import json
import os
import re

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.dirname(BASE_DIR)
PROJECT_DIR = os.path.dirname(BACKEND_DIR)
EXERCISES_DIR = PROJECT_DIR
OUTPUT_DIR = os.path.join(BACKEND_DIR, "content", "english")


def determine_subject_key(sentence: str) -> str:
    """Extract the subject part before the blank to determine answer."""
    before_blank = sentence.split("______")[0].strip()
    before_blank = before_blank.rstrip()
    return before_blank


AFFIRMATIVE_RULES = {
    "I": "am",
    "i": "am",
    "You": "are",
    "you": "are",
    "He": "is",
    "he": "is",
    "She": "is",
    "she": "is",
    "It": "is",
    "it": "is",
    "We": "are",
    "we": "are",
    "They": "are",
    "they": "are",
    "Everyone": "is",
    "everyone": "is",
}

# Plural indicators (nouns ending in s, "and", etc.)
PLURAL_INDICATORS = [
    " and ", "houses", "keys", "flowers", "children", "books",
    "friends", "dogs", "teachers", "parents", "sisters",
    "apples", "windows",
]

# Singular names from the exercises
SINGULAR_NAMES = [
    "Claudia", "Manuel", "Natalia", "David", "Dagui",
    "Vanesa", "The girl", "The cat", "The garden",
    "The teacher", "The car", "The house", "The weather",
    "The window", "The sky",
]

# Special cases
SPECIAL_RULES = {
    "David and Dagui": "are",
    "Claudia and Manuel": "are",
    "Manuel and Vanesa": "are",
    "Vanesa and Natalia": "are",
    "Natalia and I": "are",
    "You and I": "are",
    "Claudia and I": "are",
    "My brother Natalia": "is",
    "your brother Natalia": "is",
    "your father Manuel": "is",
    "The dogs": "are",
    "The books": "are",
    "My friends": "are",
    "The houses": "are",
    "The keys": "are",
    "The flowers": "are",
    "The children": "are",
    "The apples": "are",
    "The windows": "are",
    "The houses": "are",
    "Everyone": "is",
    "the children": "are",
    "The children": "are",
    "everyone": "is",
    "the weather": "is",
    "The weather": "is",
    "the girl": "is",
    "The girl": "is",
    "the cat": "is",
    "The cat": "is",
    "the car": "is",
    "The car": "is",
    "the house": "is",
    "The house": "is",
    "the garden": "is",
    "The garden": "is",
    "the teacher": "is",
    "The teacher": "is",
    "the window": "is",
    "The window": "is",
    "the sky": "is",
    "The sky": "is",
    "the keys": "are",
    "The keys": "are",
    "the books": "are",
    "The books": "are",
    "the flowers": "are",
    "The flowers": "are",
    "the dogs": "are",
    "The dogs": "are",
    "the apples": "are",
    "The apples": "are",
    "My brother Natalia": "is",
    "my friends": "are",
    "My friends": "are",
}


def get_affirmative_answer(sentence: str) -> str:
    """Determine am/is/are for a fill-blank exercise."""
    before = sentence.split("______")[0].strip()

    # Check special rules first
    for key, value in SPECIAL_RULES.items():
        if key in before:
            return value

    # Check first word for pronoun rules
    first_word = before.split()[0] if before.split() else ""
    first_lower = first_word.lower()

    if first_lower in AFFIRMATIVE_RULES:
        return AFFIRMATIVE_RULES.get(first_word, AFFIRMATIVE_RULES.get(first_lower, "is"))

    # Check for "and" (plural)
    if " and " in before:
        return "are"

    # Check plural indicators
    for indicator in PLURAL_INDICATORS:
        if indicator.lower() in before.lower():
            return "are"

    # Default to is for singular
    return "is"


NEGATIVE_RULES = {
    "I": "am not",
    "You": "aren't",
    "He": "isn't",
    "She": "isn't",
    "It": "isn't",
    "We": "aren't",
    "They": "aren't",
    "Everyone": "isn't",
}

NEGATIVE_SPECIAL = {
    "David and Dagui": "aren't",
    "Claudia and Manuel": "aren't",
    "Manuel and Vanesa": "aren't",
    "Vanesa and Natalia": "aren't",
    "Claudia and I": "aren't",
    "David and Dagui": "aren't",
    "My brother Natalia": "isn't",
    "Manuel and Vanesa": "aren't",
    "The dogs": "aren't",
    "The books": "aren't",
    "The children": "aren't",
    "The apples": "aren't",
    "The windows": "aren't",
    "The flowers": "aren't",
    "The keys": "aren't",
    "The houses": "aren't",
    "Everyone": "isn't",
    "the weather": "isn't",
    "The weather": "isn't",
    "The car": "isn't",
    "The house": "isn't",
    "The window": "isn't",
    "The cat": "isn't",
    "The garden": "isn't",
    "The girl": "isn't",
    "The teacher": "isn't",
}


def get_negative_answer(sentence: str) -> str:
    """Determine am not/isn't/aren't for negative exercises."""
    before = sentence.split("______")[0].strip()

    for key, value in NEGATIVE_SPECIAL.items():
        if key in before:
            return value

    first_word = before.split()[0] if before.split() else ""
    first_lower = first_word.lower()

    if first_lower in {"he", "she", "it"}:
        return "isn't"
    if first_lower in {"you", "we", "they"}:
        return "aren't"
    if first_lower == "i":
        return "am not"

    if " and " in before:
        return "aren't"

    for indicator in PLURAL_INDICATORS:
        if indicator.lower() in before.lower():
            return "aren't"

    return "isn't"


INTERROGATIVE_SPECIAL = {
    "David and Dagui": "Are",
    "Claudia and Manuel": "Are",
    "Manuel and Vanesa": "Are",
    "Vanesa and Natalia": "Are",
    "Natalia and I": "Are",
    "you and I": "Are",
    "Claudia and I": "Are",
    "My brother Natalia": "Is",
    "your brother Natalia": "Is",
    "the dogs": "Are",
    "the books": "Are",
    "the children": "Are",
    "the apples": "Are",
    "the windows": "Are",
    "the flowers": "Are",
    "the keys": "Are",
    "the houses": "Are",
    "Everyone": "Is",
    "everyone": "Is",
    "the weather": "Is",
    "The weather": "Is",
    "The car": "Is",
    "The house": "Is",
    "The window": "Is",
    "The cat": "Is",
    "The garden": "Is",
    "The girl": "Is",
    "The teacher": "Is",
    "The sky": "Is",
    "The dogs": "Are",
    "The books": "Are",
    "The children": "Are",
    "The apples": "Are",
    "The windows": "Are",
    "The flowers": "Are",
    "The keys": "Are",
    "The houses": "Are",
    "the girl": "Is",
    "the cat": "Is",
    "the car": "Is",
    "the house": "Is",
    "the garden": "Is",
    "the teacher": "Is",
    "the window": "Is",
    "the sky": "Is",
}


def get_interrogative_answer(sentence: str) -> str:
    """Determine Am/Is/Are for interrogative exercises."""
    after = sentence.split("______")[1].strip() if "______" in sentence else sentence.strip()
    # The blank is at the beginning, so look at what comes after
    after = after.lstrip()

    for key, value in INTERROGATIVE_SPECIAL.items():
        if key in after:
            return value

    first_word = after.split()[0] if after.split() else ""

    if first_word.lower() == "i":
        return "Am"
    if first_word.lower() in {"he", "she", "it"}:
        return "Is"
    if first_word.lower() in {"you", "we", "they"}:
        return "Are"

    if " and " in after:
        return "Are"

    for indicator in PLURAL_INDICATORS:
        if indicator.lower() in after.lower():
            return "Are"

    return "Is"


def get_short_answer(sentence: str, positive: bool = True) -> tuple[str, str]:
    """Determine short answer. Returns (answer, hint)."""
    question = sentence.split("—")[0].strip()

    # Extract subject from question
    subject_map = {
        "Are you": ("I", "you", "I am not" if not positive else "I am"),
        "Is he": ("he", "he isn't" if not positive else "he is"),
        "Is she": ("she", "she isn't" if not positive else "she is"),
        "Is it": ("it", "it isn't" if not positive else "it is"),
        "Are they": ("they", "they aren't" if not positive else "they are"),
        "Are we": ("we", "we aren't" if not positive else "we are"),
        "Am I": ("I", "I'm not" if not positive else "I am"),
        "Is Claudia": ("she", "she isn't" if not positive else "she is"),
        "Is Manuel": ("he", "he isn't" if not positive else "he is"),
        "Is Natalia": ("she", "she isn't" if not positive else "she is"),
        "Is Vanesa": ("she", "she isn't" if not positive else "she is"),
        "Is David": ("he", "he isn't" if not positive else "he is"),
        "Is Dagui": ("he", "he isn't" if not positive else "he is"),
        "Are David and Dagui": ("they", "they aren't" if not positive else "they are"),
        "Are Natalia and I": ("you", "you aren't" if not positive else "you are"),
        "Are the children": ("they", "they aren't" if not positive else "they are"),
        "Are the windows": ("they", "they aren't" if not positive else "they are"),
        "Are the books": ("they", "they aren't" if not positive else "they are"),
        "Are the keys": ("they", "they aren't" if not positive else "they are"),
        "Are the apples": ("they", "they aren't" if not positive else "they are"),
        "Is the house": ("it", "it isn't" if not positive else "it is"),
        "Is the car": ("it", "it isn't" if not positive else "it is"),
        "Is the cat": ("it", "it isn't" if not positive else "it is"),
        "Is the sky": ("it", "it isn't" if not positive else "it is"),
        "Is the teacher": ("he" if not positive else "he is", "he/she isn't" if not positive else "he/she is"),
        "Are you and I": ("we", "we aren't" if not positive else "we are"),
        "Is everyone": ("they", "they aren't" if not positive else "they are"),
        "Is your father Manuel": ("he", "he isn't" if not positive else "he is"),
        "Is your brother Natalia": ("he", "he isn't" if not positive else "he is"),
        "Are you": ("I", "I am not" if not positive else "I am"),
        "Is she beautiful": ("she", "she isn't" if not positive else "she is"),
    }

    for key, (pronoun, answer) in subject_map.items():
        if question.startswith(key):
            # Check if the question expects positive or negative
            if "— Yes" in sentence:
                return answer.split(" aren't")[0].split(" isn't")[0].split(" am not")[0], f"{pronoun} " + ("is" if pronoun in ("he", "she", "it") else "are" if pronoun in ("we", "they") else "am")
            if "— No" in sentence:
                return answer, f"Negar con {answer}"

    # Fallback
    if positive:
        return "I am", ""
    return "I am not", ""


def parse_affirmative_file(filepath: str) -> list[dict]:
    """Parse Ejercicios_1 (affirmative) file."""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    blocks = []
    current_block = None
    current_title = ""

    lines = content.split("\n")
    for line in lines:
        line = line.strip()
        if not line or "EXPLICACIÓN" in line or "TRUCO" in line or "La tabla" in line:
            continue
        if "En inglés usamos" in line or "Si el sujeto" in line:
            continue
        if "PASO" in line or "Parte" in line:
            current_title = line.replace(":", "").strip()
            current_block = {"title": current_title, "items": []}
            blocks.append(current_block)
            continue
        if "______" in line and current_block is not None:
            answer = get_affirmative_answer(line)
            hint = ""
            if answer == "am":
                hint = "AM → Solo YO"
            elif answer == "is":
                hint = "IS → Uno solo"
            elif answer == "are":
                hint = "ARE → Varios o YOU"
            current_block["items"].append({
                "sentence": line,
                "answer": answer,
                "hint": hint,
            })

    return blocks


def parse_negative_file(filepath: str) -> list[dict]:
    """Parse Ejercicios_2 (negative) file."""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    blocks = []
    current_block = None
    current_title = ""

    lines = content.split("\n")
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if "EXPLICACIÓN" in line or "Antes decías" in line or "TRUCO" in line or "ABREVIACIONES" in line:
            continue
        if "Para decir" in line or "Ahora para" in line or "I am" in line or "He / She" in line:
            continue
        if "│" in line or "|" in line:
            continue
        if "BLOQUE" in line:
            current_title = line.replace(":", "").strip()
            current_block = {"title": current_title, "items": []}
            blocks.append(current_block)
            continue
        if "______" in line and current_block is not None:
            answer = get_negative_answer(line)
            hint = ""
            if answer == "am not":
                hint = "am not → Solo para I"
            elif answer == "isn't":
                hint = "isn't = is + not → Uno solo"
            elif answer == "aren't":
                hint = "aren't = are + not → Varios o YOU"
            current_block["items"].append({
                "sentence": line,
                "answer": answer,
                "hint": hint,
            })

    return blocks


def parse_interrogative_file(filepath: str) -> list[dict]:
    """Parse Ejercicios_3 (interrogative) file."""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    blocks = []
    current_block = None
    current_title = ""

    lines = content.split("\n")
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if "La regla" in line or "Objetivo" in line or "Afirmación" in line or "Para hacer" in line:
            continue
        if "You are happy" in line or "He is at home" in line:
            continue
        if "│" in line or "|" in line:
            continue
        if "BLOQUE" in line:
            current_title = line.replace(":", "").strip()
            current_block = {"title": current_title, "items": []}
            blocks.append(current_block)
            continue
        if "______" in line and current_block is not None:
            answer = get_interrogative_answer(line)
            hint = f"{answer} → {'Yo' if answer == 'Am' else 'Uno solo' if answer == 'Is' else 'Varios/YOU'}"
            current_block["items"].append({
                "sentence": line,
                "answer": answer,
                "hint": hint,
            })

    return blocks


def parse_short_answers_file(filepath: str) -> list[dict]:
    """Parse Ejercicios_4 (short answers) file."""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    blocks = []
    current_block = None
    current_title = ""
    current_item = None

    lines = content.split("\n")
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if "EXPLICACIÓN" in line or "La regla" in line or "Objetivo" in line or "Pregunta empieza" in line:
            continue
        if "│" in line or "|" in line:
            continue
        if "BLOQUE" in line:
            current_title = line.replace(":", "").strip()
            current_block = {"title": current_title, "items": []}
            blocks.append(current_block)
            continue
        if "—" in line:
            # Question line
            is_positive = "— Yes" in line
            question = line.split("—")[0].strip()

            # Determine answer
            answer = _determine_short_answer(line)
            hint = _get_short_answer_hint(question)

            current_item = {
                "sentence": line,
                "answer": answer,
                "hint": hint,
            }
            if current_block is not None:
                current_block["items"].append(current_item)

    return blocks


def _determine_short_answer(line: str) -> str:
    """Determine the correct short answer for a question."""
    question_part = line.split("—")[0].strip()
    is_positive = "— Yes" in line

    # Map question to pronoun
    pronoun_map = {
        "you": "you" if "you and I" not in question_part.lower() and "Are you" in question_part else ("we" if "you and I" in question_part.lower() else "you"),
    }

    # Determine subject pronoun from question
    if question_part.startswith("Am I"):
        return "I am" if is_positive else "I'm not"
    elif question_part.startswith("Is he") or question_part.startswith("Is Manuel") or question_part.startswith("Is David") or question_part.startswith("Is Dagui"):
        return "he is" if is_positive else "he isn't"
    elif question_part.startswith("Is she") or question_part.startswith("Is Claudia") or question_part.startswith("Is Natalia") or question_part.startswith("Is Vanesa"):
        return "she is" if is_positive else "she isn't"
    elif question_part.startswith("Is it") or question_part.startswith("Is the car") or question_part.startswith("Is the cat") or question_part.startswith("Is the house") or question_part.startswith("Is the sky"):
        return "it is" if is_positive else "it isn't"
    elif question_part.startswith("Are you and I") or question_part.startswith("Are Natalia and I"):
        return "we are" if is_positive else "we aren't"
    elif question_part.startswith("Are we"):
        return "we are" if is_positive else "we aren't"
    elif question_part.startswith("Are you"):
        return "I am" if is_positive else "I'm not"
    elif question_part.startswith("Are they") or question_part.startswith("Are the children") or question_part.startswith("Are the windows") or question_part.startswith("Are the books") or question_part.startswith("Are the keys") or question_part.startswith("Are David and Dagui") or question_part.startswith("Are the apples"):
        return "they are" if is_positive else "they aren't"
    elif question_part.startswith("Is everyone"):
        return "they are" if is_positive else "they aren't"
    elif question_part.startswith("Is your father") or question_part.startswith("Is your brother"):
        return "he is" if is_positive else "he isn't"
    elif question_part.startswith("Is the teacher"):
        return "he is" if is_positive else "he isn't"
    elif question_part.startswith("Is she beautiful"):
        return "she is" if is_positive else "she isn't"
    elif question_part.startswith("Are you sure") or question_part.startswith("Are you tired"):
        return "I am" if is_positive else "I'm not"
    elif question_part.startswith("Is it raining") or question_part.startswith("Is it easy") or question_part.startswith("Is it ten"):
        return "it is" if is_positive else "it isn't"
    elif question_part.startswith("Are you from"):
        return "I am" if is_positive else "I'm not"
    elif question_part.startswith("Are you a"):
        return "I am" if is_positive else "I'm not"
    elif question_part.startswith("Is Manuel at"):
        return "he is" if is_positive else "he isn't"
    elif question_part.startswith("Is Claudia"):
        return "she is" if is_positive else "she isn't"
    elif question_part.startswith("Are we"):
        return "we are" if is_positive else "we aren't"
    elif question_part.startswith("Are they"):
        return "they are" if is_positive else "they aren't"

    return "Yes" if is_positive else "No"


def _get_short_answer_hint(question: str) -> str:
    """Get a hint for the short answer."""
    if "you" in question.lower() and "are you" in question.lower():
        return "Are you...? → Yes, I am / No, I'm not"
    if "he" in question.lower() or "manuel" in question.lower() or "david" in question.lower():
        return "Is he...? → Yes, he is / No, he isn't"
    if "she" in question.lower() or "claudia" in question.lower() or "natalia" in question.lower():
        return "Is she...? → Yes, she is / No, she isn't"
    if "it" in question.lower() or "car" in question.lower() or "cat" in question.lower() or "house" in question.lower() or "sky" in question.lower():
        return "Is it...? → Yes, it is / No, it isn't"
    if "they" in question.lower() or "children" in question.lower() or "books" in question.lower() or "david and" in question.lower():
        return "Are they...? → Yes, they are / No, they aren't"
    if "we" in question.lower():
        return "Are we...? → Yes, we are / No, we aren't"
    if "am i" in question.lower():
        return "Am I...? → Yes, I am / No, I'm not"
    return ""


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Parse all files
    affirmative_blocks = parse_affirmative_file(
        os.path.join(EXERCISES_DIR, "Ejercicios_1(Afirmative)")
    )
    negative_blocks = parse_negative_file(
        os.path.join(EXERCISES_DIR, "Ejercicios_2(Negative)")
    )
    interrogative_blocks = parse_interrogative_file(
        os.path.join(EXERCISES_DIR, "Ejercicios_3(Interrogative)")
    )
    short_answer_blocks = parse_short_answers_file(
        os.path.join(EXERCISES_DIR, "Ejercicios_4(Answer)")
    )

    # Count total items per unit
    affirmative_count = sum(len(b["items"]) for b in affirmative_blocks)
    negative_count = sum(len(b["items"]) for b in negative_blocks)
    interrogative_count = sum(len(b["items"]) for b in interrogative_blocks)
    short_count = sum(len(b["items"]) for b in short_answer_blocks)

    verb_to_be = {
        "id": "verb-to-be",
        "name": "Verbo To Be",
        "subjectId": "english",
        "order": 1,
        "difficulty": 1,
        "icon": "📝",
        "theory": {
            "text": "En inglés usamos AM - IS - ARE para decir soy, estoy, es, eres, somos, están. Para negar añadimos NOT (am not, isn't, aren't). Para preguntar, invertimos el orden: verbo + sujeto. Y para responder corto usamos Yes/No + pronombre + verbo.",
            "table": {
                "headers": ["Si el sujeto es...", "Usa...", "Ejemplo"],
                "rows": [
                    ["I (yo)", "am", "I am happy"],
                    ["He / She / It (uno solo)", "is", "She is a doctor"],
                    ["You / We / They (varios o 'tú')", "are", "We are friends"],
                    ["Un nombre solo: Claudia", "is", "Claudia is smart"],
                    ["Dos nombres: David and Dagui", "are", "They are outside"],
                ],
            },
            "tips": [
                {"emoji": "👑", "text": "AM → Solo YO. Como un rey solitario"},
                {"emoji": "👉", "text": "IS → Apuntas con el dedo a UNO → ¡eso IS!"},
                {"emoji": "🚢", "text": "ARE → Una ARmada de MUCHOS barcos"},
                {"emoji": "❌", "text": "Para negar: am not (I), isn't (uno), aren't (varios)"},
                {"emoji": "❓", "text": "Para preguntar: cambia verbo y sujeto de lugar"},
                {"emoji": "✅", "text": "Respuesta corta: Yes/No + pronombre + am/is/are"},
            ],
        },
        "units": [
            {
                "id": "affirmative",
                "title": "Afirmativo (am / is / are)",
                "exerciseType": "fill-blank",
                "explanation": "Completa con am, is o are según corresponda. Recuerda: AM → solo YO, IS → uno solo, ARE → varios o YOU.",
                "blocks": affirmative_blocks,
            },
            {
                "id": "negative",
                "title": "Negativo (am not / isn't / aren't)",
                "exerciseType": "fill-blank",
                "explanation": "Para decir NO SOY, NO ESTOY, NO ES... solo añade NOT después de am, is, are. I am not → no se encoge. He is not = isn't. You are not = aren't.",
                "blocks": negative_blocks,
            },
            {
                "id": "interrogative",
                "title": "Interrogativo (Am / Is / Are ...?)",
                "exerciseType": "fill-blank",
                "explanation": "Para hacer preguntas, cambia el verbo (am/is/are) y el sujeto de lugar. Empieza con el verbo. Ejemplo: You are happy → Are you happy?",
                "blocks": interrogative_blocks,
            },
            {
                "id": "short-answers",
                "title": "Respuestas cortas (Yes/No)",
                "exerciseType": "fill-blank",
                "explanation": "Para responder sin repetir toda la frase, usas: Yes + pronombre + am/is/are o No + pronombre + am not/isn't/aren't. El pronombre debe concordar con el sujeto de la pregunta.",
                "blocks": short_answer_blocks,
            },
        ],
        "testConfig": {
            "totalQuestions": 20,
            "shuffle": True,
            "includeUnits": ["affirmative", "negative", "interrogative", "short-answers"],
        },
    }

    # Write JSON
    output_path = os.path.join(OUTPUT_DIR, "verb-to-be.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(verb_to_be, f, ensure_ascii=False, indent=2)

    print(f"Generated {output_path}")
    print(f"  Affirmative: {affirmative_count} items in {len(affirmative_blocks)} blocks")
    print(f"  Negative: {negative_count} items in {len(negative_blocks)} blocks")
    print(f"  Interrogative: {interrogative_count} items in {len(interrogative_blocks)} blocks")
    print(f"  Short Answers: {short_count} items in {len(short_answer_blocks)} blocks")

    # Write subjects.json
    content_dir = os.path.join(BACKEND_DIR, "content")
    subjects_path = os.path.join(content_dir, "subjects.json")
    os.makedirs(content_dir, exist_ok=True)

    subjects = {
        "subjects": [
            {
                "id": "english",
                "name": "Inglés",
                "icon": "📚",
                "color": "#4CAF50",
                "mapConfig": None,
            }
        ]
    }

    with open(subjects_path, "w", encoding="utf-8") as f:
        json.dump(subjects, f, ensure_ascii=False, indent=2)

    print(f"Generated {subjects_path}")


if __name__ == "__main__":
    main()
