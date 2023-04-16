import random
import math
import imageio
import numpy
from numpy.fft import fft2, fftshift
from scipy.stats.stats import pearsonr
from scipy.fftpack import dct
from skimage.util import view_as_blocks

def main(path, key, length):
    image = imageio.imread(path, as_gray=True)
    cvz=randomVector(key, length)
    
    b=decodeWatermark(image, 57846874321257)
    watermark=makeWatermark(image.shape, int(min(image.shape)/4), 57846874321257, 16)
    valuesG=applyWatermark(image, watermark[0], 0, watermark[1])
    #min1=0.0
    #max1=1.0
    #for i in range(len(b)):
    #    val=(b[i]-min1)/(max1-min1)
    #    b[i]=int(val)
    #num=int(number)
    #return randomVector(57846874321257, 16)
#    return pearsonr(valuesG, b)[0]
    #return indicesG

def makeWatermark(imageShape, radius, secretKey, vectorLength=16):
    indicesG=[]
    watermark = numpy.zeros(imageShape)
    center = (int(imageShape[0] / 2) + 1, int(imageShape[1] / 2) + 1)

    vector = randomVector(secretKey, vectorLength)

    indices = [watermarkValue(center,
                              t,
                              vectorLength,
                              int(radius)) for t in range(vectorLength)]

    for i, location in enumerate(indices):
        watermark[location] = vector[i]
        indicesG.append(location)

    return [watermark, indicesG]

def watermarkValue(center, value, vectorLength, radius):
    return (center[0] + int(radius *
                            math.cos(2 * value * math.pi / vectorLength)),
            center[1] + int(radius *
                            math.sin(2 * value * math.pi / vectorLength)))

def randomVector(seed, length):
    #random.seed(seed)
    #return [random.choice([0, 1]) for _ in range(length)]
    numpy.random.seed(seed)
    lst=numpy.random.randint(0, 2, length)
    return list(lst)
    
def decodeWatermark(image, secretKey):
    center = (int(image.shape[0] / 2) + 1, int(image.shape[1] / 2) + 1)
    radius=int(min(image.shape)/4)
    indices=[watermarkValue(center, t, 16, int(radius)) for t in range(16)]
    shiftedDFT = fftshift(fft2(image))
    vector=[]
    vectorOrig=randomVector(secretKey, 16)
    for i, location in enumerate(indices):
        vector.append(numpy.real(shiftedDFT[location]))
    return vector

def applyWatermark(imageMatrix, watermarkMatrix, alpha, indicesG):
    valuesG=[]
    shiftedDFT = fftshift(fft2(imageMatrix))
    watermarkedDFT = shiftedDFT + alpha * watermarkMatrix
    for i in indicesG:
        valuesG.append(numpy.real(watermarkedDFT[i]))
    return valuesG
    
def testFuncFromArticle(a,b):
    bM=numpy.mean(b)/len(b)
    bSTD=numpy.std(b)/len(b)
    res=[]
    k=1
    for i in range(len(b)):
        if(b[i]>=bM+bSTD*k):
            res.append(1)
        else:
            res.append(0)
    return pearsonr(a,res)
    
def testFuncRadius(secretKey, vectorLength, img):
    center=(int(img.shape[0]/2)+1, int(img.shape[1]/2)+1)
    radius=(min(img.shape)/4)+2
    indices=[watermarkValue(center, t, vectorLength, int(radius)) for t in range(vectorLength)]
    res=[]
    for location in indices:
        res.append(img[location])

def crossCor(a,b,n):
    resCor=[]
    aM=numpy.mean(a)
    bM=numpy.mean(b)
    for j in range(n):
        up=0.0
        down=0.0
        down1=0.0
        down2=0.0
        for i in range(n):
            up+=(a[i]-aM)*(b[i+j]-bM)
            down1+=(a[i]-aM)**2
            down2+=(b[i+j]-bM)**2
        down=math.sqrt(down1*down2)
        resCor.append(up/down)
    return resCor

def aroundCircle(img):
    radius=(min(img.shape)/4)
    center=(int(img.shape[0]/2)+1, int(img.shape[1]/2)+1)
    imgDFT=fftshift(fft2(img))
    theta=numpy.linspace(0, 2*numpy.pi, 100)
    x=center[0]+radius*numpy.cos(theta)
    y=center[1]+radius*numpy.sin(theta)
    res=[]
    for i in range(len(x)):
        res.append(numpy.real(imgDFT[int(x[i])][int(y[i])]))
    return res

def abs_diff_coefsDFT(a, b):
    return abs(a)-abs(b)
def get_bitDFT(block, threshold):
    allDiffs=[]
    for i in range(len(block)):
        for j in range(len(block)):
            allDiffs.append(abs_diff_coefsDFT(block[i], block[j]))
    if(max(allDiffs)>threshold):
        return 1
    else:
        return 0
def checkNearPixels(img, i, j):
    shiftedDFT=fftshift(fft2(img))
    res=[]
    x1=shiftedDFT[i][j]
    x2=shiftedDFT[i][j-1]
    x3=shiftedDFT[i][j+1]
    x4=shiftedDFT[i-1][j]
    x5=shiftedDFT[i-1][j-1]
    x6=shiftedDFT[i-1][j+1]
    x7=shiftedDFT[i+1][j]
    x8=shiftedDFT[i+1][j-1]
    x9=shiftedDFT[i+1][j+1]
    res.append(numpy.real(x1))
    res.append(numpy.real(x2))
    res.append(numpy.real(x3))
    res.append(numpy.real(x4))
    res.append(numpy.real(x5))
    res.append(numpy.real(x6))
    res.append(numpy.real(x7))
    res.append(numpy.real(x8))
    res.append(numpy.real(x9))
    return res
def dftDetect(path, key, length):
    image=imageio.imread(path, as_gray=True)
    cvz=randomVector(key, length)
    radius=int(min(image.shape)/4)+5
    #All=aroundCircle(image)
    #res=max(crossCor(cvz, All, length))
    #return res
    imageShape=image.shape
    watermark = numpy.zeros(imageShape)
    center = (int(imageShape[0] / 2) + 1, int(imageShape[1] / 2) + 1)
    vector = randomVector(key, length)
    indices = [watermarkValue(center,
                              t,
                              length,
                              int(radius)) for t in range(length)]
    res=[]
    for i in range(len(indices)):
        res.append(get_bitDFT(checkNearPixels(image, indices[i][0], indices[i][1]), 12200)) #1170 old for alpha=1000, now alpha=30000
    return 0 if pearsonr(vector, res)[0]>0.5 else 1
    
def abs_diff_coefs(transform, u1, v1, u2, v2):
    return abs(transform[u1, v1])-abs(transform[u2, v2])

def get_bit(block, u1, v1, u2, v2):
    transform=dct(dct(block, axis=0), axis=1)
    return 0 if abs_diff_coefs(transform, u1, v1, u2, v2)>0 else 1

def get_message(img, length, n, u1, v1, u2, v2):
    blocks=view_as_blocks(img[:, :, 2], block_shape=(n, n))
    h=blocks.shape[1]
    return [get_bit(blocks[index//h, index%h], u1, v1, u2, v2) for index in range(length)]\
    
def dctDetect(path, key, length, u1, v1, u2, v2, n):
    #try:
        image1 = imageio.imread(path)
        cvz=randomVector(key, length)
        a=get_message(image1, length, n, u1, v1, u2, v2)
        res=pearsonr(cvz, list(a))[0]
        return 0 if res>0.3 else 1
    #except:
    #    return 2
def dctExtract(path, key, length, u1, v1, u2, v2, n):
    try:
        image1 = imageio.imread(path)
        cvz=randomVector(key, length)
        a=get_message(image1, length, n, u1, v1, u2, v2)
        return a
    except:
        return "Ошибка при извлечении... Проверьте размер изображения."