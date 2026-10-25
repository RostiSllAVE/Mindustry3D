"""Arrange authentic diagnostic screenshots side by side; no generated or altered game imagery."""
import argparse
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--run',required=True)
parser.add_argument('--out',type=Path,required=True)
args=parser.parse_args()
if not args.run.replace('-','').isalnum():parser.error('Use a simple existing run name')
source=ROOT/'local/standalone-runs'/args.run
args.out.mkdir(parents=True,exist_ok=False)
try:font=ImageFont.truetype('arial.ttf',24)
except OSError:font=ImageFont.load_default()
for scene in ['sparse','mountain-heavy','mixed-biome','artificial-heavy']:
    for view in ['low','high']:
        images=[]
        for style in ['smooth','blocky','hybrid']:
            with Image.open(source/f'{scene}-{view}-{style}.png') as im:images.append(im.convert('RGB'))
        width,height=images[0].size
        if any(im.size!=(width,height) for im in images):raise ValueError('Matching viewports required')
        board=Image.new('RGB',(width*3,height+44),(24,28,35));draw=ImageDraw.Draw(board)
        for i,(label,im) in enumerate(zip(['Smooth','Blocky','Hybrid'],images)):
            draw.text((i*width+16,8),f'{label} | {scene} / {view}',font=font,fill='white');board.paste(im,(i*width,44));im.close()
        board.save(args.out/f'{scene}-{view}-comparison.png');board.close()
print(f'8 comparison panels from unchanged screenshots: {args.out}')
