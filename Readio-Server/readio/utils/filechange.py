# from mobi import Mobi
import os
import chardet

import ebooklib
import mobi
from bs4 import BeautifulSoup
from ebooklib import epub
from pdfminer.converter import PDFPageAggregator
from pdfminer.layout import LAParams
from pdfminer.pdfdocument import PDFDocument
from pdfminer.pdfinterp import PDFResourceManager, PDFPageInterpreter
from pdfminer.pdfpage import PDFPage
from pdfminer.pdfparser import PDFParser


class Chapter(object):
    # 初始化
    def __init__(self, name, text):
        self.Name = name
        self.Text = text

    # 输出章节信息
    def print_info(self):
        print("章节名称：")
        print(self.Name)
        print("章节内容：")
        print(self.Text)

    def to_dict(self):
        return {
            'ChapterName': self.Name,
            'Text': self.Text,
        }


class FileChangeSys(object):

    # 初始化
    def __init__(self, path):
        self.Path = path
        self.Chapter_uniform = []
        self.Name = None
        self.dot_index = None
        self.Type = None

    def decode(self, print_info=None):
        # 解码文件
        self.decode_filetype()
        # print(f'[DEBUG] 1 decode')
        if print_info:
            self.print_fileinfo()
        if self.Type == 'pdf':
            self.pdf_decode()
        elif self.Type == 'txt':
            self.txt_decode()
        elif self.Type == 'epub':
            self.epub_decode()
        elif self.Type == 'mobi':
            self.mobi_decode()
        else:
            print("不支持的文件类型")
        # print(f'[DEBUG] 2 decode')
        return [c.to_dict() for c in self.Chapter_uniform]

    # 解码Path
    def decode_filetype(self):
        self.Name = os.path.basename(self.Path)
        self.dot_index = self.Name.find('.')
        self.Type = self.Name[self.dot_index + 1:].lower()

    # print文件信息
    def print_fileinfo(self):
        print("Filepath:")
        print(self.Path)
        print("Filename:")
        print(self.Name)
        print("Filetype:")
        print(self.Type)

    def read_file(self, path=None):
        if path is None:
            path = self.Path
        # 使用 chardet 模块自动检测文件编码类型
        with open(path, 'rb') as f:
            result = chardet.detect(f.read(1024))
        # print(result)
        # if result['encoding'] is None:
        #     result['encoding'] = 'utf-8'
        # 根据检测结果打开文件并读取内容
        with open(path, 'r', encoding=result['encoding']) as f:
            content = f.read()

        return content

    # 如果是pdf类型，直接终端输出
    def pdf_decode(self):
        # 获取文档
        fp = open(self.Path, 'rb')
        # 创建解释器
        pdf_parser = PDFParser(fp)
        # PDF文档对象
        doc = PDFDocument(pdf_parser)
        # 连接解释器和文档对象
        pdf_parser.set_document(doc)
        # doc.set_parser(pdf_parser)

        # 初始化文档
        # doc.initialize()

        # 创建PDF资源管理器
        resource = PDFResourceManager()
        # 创建一个PDF参数分析器
        laparam = LAParams()
        # 创建聚合器
        device = PDFPageAggregator(resource, laparams=laparam)
        # 创建PDF页面解析器
        interpreter = PDFPageInterpreter(resource, device)
        # 循环遍历列表，每次处理一页的内容
        # doc.get_pages() 获取page列表
        page_index = 0  # 统计页数
        for page in PDFPage.create_pages(doc):
            # 使用页面解释器来读取
            page_index += 1
            new_chapter_name = "第" + str(page_index) + "页"
            interpreter.process_page(page)
            # 使用聚合器获得内容
            new_chapter_text = ""
            layout = device.get_result()
            for out in layout:
                if hasattr(out, 'get_text'):
                    new_chapter_text += out.get_text()
            new_chapter = Chapter(new_chapter_name, new_chapter_text)
            self.Chapter_uniform.append(new_chapter)
            # new_chapter.print_info()

    # txt文档转pdf文档
    def txt_decode(self):
        # 暂时：章节名就是书名 -> None
        # new_chapter_name = self.Name
        new_chapter_name = None
        new_chapter_text = self.read_file()
        # print(new_chapter_text)
        # 合成一个章节
        new_chapter = Chapter(new_chapter_name, new_chapter_text)
        self.Chapter_uniform.append(new_chapter)

    # epub转pdf
    def epub_decode(self):
        # print(f'[DEBUG] epub_decode: {self.Path}')
        book = epub.read_epub(self.Path, options={'ignore_ncx': True})
        index = 0
        # print(f'[DEBUG] epub.read_epub')
        for text in book.get_items_of_type(ebooklib.ITEM_DOCUMENT):
            index += 1
            # soup = BeautifulSoup(text.get_content(), 'html')
            soup = BeautifulSoup(text.get_content(), features='xml')  # 显式指定使用 xml 解析器
            new_chapter_text = ''
            for item in soup.find_all("p"):
                # print(item.text)
                new_chapter_text += item.text
                new_chapter_text += '\n'
            if new_chapter_text.isspace():
                new_chapter_text = "该章节可能仅由图片构成"

            new_chapter_name = "第" + str(index) + "章"
            # 整合成章节类
            new_chapter = Chapter(new_chapter_name, new_chapter_text)
            # 加入list
            self.Chapter_uniform.append(new_chapter)
            # print下信息
            # new_chapter.print_info()
        # print(f'[DEBUG] end of epub_decode')

    # mobi转pdf
    def mobi_decode(self):
        tempdir, filepath = mobi.extract(self.Path)
        # new_chapter_name = self.Name
        new_chapter_name = None
        # 获取章节内容
        # f = open(filepath, 'r', encoding='utf-8')
        # html = f.read()
        html = self.read_file(path=filepath)
        soup = BeautifulSoup(html, 'lxml')
        new_chapter_text = ''
        for item in soup.find_all("p"):
            new_chapter_text += item.text
            new_chapter_text += '\n'
        if new_chapter_text.isspace():
            new_chapter_text = "该章节可能仅由图片构成"
        # 整合成章节类
        new_chapter = Chapter(new_chapter_name, new_chapter_text)
        # 加入list
        self.Chapter_uniform.append(new_chapter)
