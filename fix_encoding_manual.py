import os
import re

# Dictionary mapping common broken Vietnamese patterns to correct ones
# Focused on patterns where bytes were replaced by spaces or other mangled chars
replacements = {
    r'Ä ANG': 'ĐANG',
    r'CHáº\xa0Y': 'CHẠY',
    r'Táº\xa0I': 'TẠI',
    r'Ä \x81': 'Đ',
    r'Ä ': 'Đ',
    r'Ä\x83': 'ă',
    r'Ã ': 'à',
    r'Ã¡': 'á',
    r'áº£': 'ả',
    r'Ã£': 'ã',
    r'áº¡': 'ạ',
    r'Ãª': 'ê',
    r'á»\x81': 'ề',
    r'áº¿': 'ế',
    r'á»\x83': 'ể',
    r'á»\x85': 'ễ',
    r'á»\x87': 'ệ',
    r'Ã´': 'ô',
    r'á»\x93': 'ồ',
    r'á»\x91': 'ố',
    r'á»\x95': 'ổ',
    r'á»\x97': 'ỗ',
    r'á»\x99': 'ộ',
    r'Æ°': 'ư',
    r'á»«': 'ừ',
    r'á»©': 'ứ',
    r'á»\xad': 'ử',
    r'á»¯': 'ữ',
    r'á»±': 'ự',
    r'Ã\xad': 'í',
    r'Ã¬': 'ì',
    r'Ã²': 'ò',
    r'Ã³': 'ó',
    r'á»\x8f': 'ỏ',
    r'Ãµ': 'õ',
    r'á»\x8d': 'ọ',
    r'Ã¹': 'ù',
    r'Ãº': 'ú',
    r'á»§': 'ủ',
    r'Å©': 'ũ',
    r'á»¥': 'ụ',
    r'Ã½': 'ý',
    r'á»³': 'ỳ',
    r'á»·': 'ỷ',
    r'á»¹': 'ỹ',
    r'á»µ': 'ỵ',
    r'Ä\x91': 'đ',
    r'Ä\x90': 'Đ',
    r'Ä\xa0': 'Đ',
    r'nháº\xadp': 'nhập',
    r'thÃ\xa0nh c\xc3\xb4ng': 'thành công',
    r'tháº¥t báº¡i': 'thất bại',
    r'káº¿t ná»\x91i': 'kết nối',
    r'Ä\x83': 'ă',
    r'Ã¢': 'â',
    r'Ã\xaa': 'ê',
    r'Ã´': 'ô',
    r'Æ¡': 'ơ',
    r'Æ°': 'ư',
}

def fix_manually(content):
    res = content
    # Order replacements by length descending to avoid partial matches
    for pattern in sorted(replacements.keys(), key=len, reverse=True):
        res = res.replace(pattern, replacements[pattern])
    
    # Fix the specific "Ä ã" case
    res = res.replace("Ä ã", "Đã")
    res = res.replace("Ä  ", "Đ")
    
    return res

src_dirs = ['src/main/java', 'src/test/java', 'src/main/resources']
for target_dir in src_dirs:
    if not os.path.exists(target_dir): continue
    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if file.lower().endswith(('.java', '.fxml', '.html', '.css')):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    fixed = fix_manually(content)
                    if fixed != content:
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(fixed)
                        print(f"Manually Fixed: {path}")
                except:
                    pass
