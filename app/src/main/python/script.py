import random
import math
import imageio
import numpy
from numpy.fft import fft2, ifft2, fftshift, ifftshift
from scipy.stats.stats import pearsonr
from scipy.fftpack import dct, idct
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
    return 0 if pearsonr(vector, res)[0]>0.3 else 1 #old=0.5, optimal=0.4
    
def abs_diff_coefs(transform, u1, v1, u2, v2):
    return abs(transform[u1, v1])-abs(transform[u2, v2])

def get_bit(block, u1, v1, u2, v2):
    transform=dct(dct(block, axis=0), axis=1)
    return 0 if abs_diff_coefs(transform, u1, v1, u2, v2)>0 else 1

def get_message(img, length, n, u1, v1, u2, v2):
    blocks=view_as_blocks(img[:, :, 2], block_shape=(n, n))
    h=blocks.shape[1]
    return [get_bit(blocks[index//h, index%h], u1, v1, u2, v2) for index in range(length)]

def double_to_byte(arr):
    return numpy.uint8(numpy.round(numpy.clip(arr, 0, 255), 0))

def increment_abs(x):
    return x+1 if x>=0 else x-1

def decrement_abs(x):
    if numpy.abs(x)<=1:
        return 0
    else:
        return x-1 if x>=0 else x+1

def valid_coeffs(transform, bit, threshold, u1, v1, u2, v2):
    difference=abs_diff_coefs(transform, u1, v1, u2, v2)
    if(bit==0 and difference>threshold):
        return True
    elif(bit==1 and difference<-threshold):
        return True
    else:
        return False

def change_coeffs(transform, bit, u1, v1, u2, v2):
    coefs=transform.copy()
    if bit==0:
        coefs[u1,v1]=increment_abs(coefs[u1,v1])
        coefs[u2,v2]=decrement_abs(coefs[u2,v2])
    elif bit==1:
        coefs[u1,v1]=decrement_abs(coefs[u1,v1])
        coefs[u2,v2]=increment_abs(coefs[u2,v2])
    return coefs

def embed_bit(block, bit, p, u1, v1, u2, v2, n):
    patch=block.copy()
    coefs=dct(dct(patch, axis=0), axis=1)
    while not valid_coeffs(coefs, bit, p, u1, v1, u2, v2) or (bit!=get_bit(patch, u1, v1, u2, v2)):
        coefs=change_coeffs(coefs, bit, u1, v1, u2, v2) # тут ошибка
        patch=double_to_byte(idct(idct(coefs, axis=0), axis=1)/(2*n)**2)
    return patch

def embedMessage(origPath, key, length, u1, v1, u2, v2, n, p):#=1):
    try:
        msg=randomVector(key, length)
        orig=imageio.imread(origPath)
        changed=orig.copy()
        blue=changed[:, :, 2]
        blocks=view_as_blocks(blue, block_shape=(n,n))
        h=blocks.shape[1]
        for index, bit in enumerate(msg):
            i=index//h
            j=index%h
            block=blocks[i,j]
            blue[i*n: (i+1)*n, j*n: (j+1)*n]=embed_bit(block, bit, p, u1, v1, u2, v2, n)
        changed[:, :, 2]=blue
    #return changed
        imageio.imsave(origPath, changed)
#        return "Success!"
        return 1
    except:
        return -1
#        return "Error"
#    return 1

def dctDetect(path, key, length, u1, v1, u2, v2, n):
    #try:
        image1 = imageio.imread(path)
        cvz=randomVector(key, length)
        a=get_message(image1, length, n, u1, v1, u2, v2)
        res=pearsonr(cvz, list(a))[0]
        return 0 if res>0.3 else 1 #old=0.5, optimal=0.4
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

def applyWatermark1(img, watermark, alpha):
    shiftedDFT=fftshift(fft2(img))
    watermarkedDFT=shiftedDFT+alpha*watermark
    watermarkImg=ifft2(ifftshift(watermarkedDFT))
    return watermarkImg

def makeWatermark1(imgShape, radius, secretKey, length=10):
    watermark=numpy.zeros(imgShape)
    center=(int(imgShape[0]/2) +1, int(imgShape[1] /2) +1)
    vector=randomVector(secretKey, length)
    indices=[watermarkValue(center, t, length, int(radius)) for t in range(length)]
    for i, location in enumerate(indices):
        watermark[location]=vector[i]
    return watermark

#def dftEncode(data, alpha, secretKey, length):#path1, alpha, secretKey, length):
#    image1=imageio.imread(path, as_gray=True)
#    watermark=makeWatermark1(image1.shape, (min(image1.shape)/4)+5, secretKey, length)
#    watermarked=numpy.real(applyWatermark1(image1, watermark, alpha))
#    #imageio.imwrite(path1, watermarked)
#    imageio.imsave(path, watermarked)
#    return 0
def dftEncode(path, alpha, secretKey, length):#path1, alpha, secretKey, length):
    image1=imageio.imread(path, as_gray=True)
    watermark=makeWatermark1(image1.shape, (min(image1.shape)/4)+5, secretKey, length)
    watermarked=numpy.real(applyWatermark1(image1, watermark, alpha))
    #imageio.imwrite(path1, watermarked)
    imageio.imsave(path, watermarked)
    return 0

def hide_data(path, secretKey, length):#, path2, secretKey, length):
    try:
        image1=imageio.imread(path, as_gray=False, pilmode="RGB")
        nBytes=image1.shape[0]*image1.shape[1]*3//8
        secretMsg="".join(str(elem) for elem in randomVector(secretKey, length))
        if len(secretMsg)<nBytes:
            dataIndex=0
            bin_secret_msg=msg_to_bin(secretMsg)
            dataLen=len(bin_secret_msg)
            for values in image1:
                for pixels in values:
                    r,g,b=msg_to_bin(pixels)
                    if dataIndex < dataLen:
                        pixels[0]=int(r[:-1]+bin_secret_msg[dataIndex], 2)
                        dataIndex+=1
                    if dataIndex < dataLen:
                        pixels[1]=int(g[:-1]+bin_secret_msg[dataIndex], 2)
                        dataIndex+=1
                    if dataIndex<dataLen:
                        pixels[2]=int(b[:-1]+bin_secret_msg[dataIndex], 2)
                        dataIndex+=1
                    if dataIndex>=dataLen:
                        break
        #imageio.imwrite(path2, image1)
        imageio.imsave(path, image1)
        return 2
        #return "Success"
    except:
        return -1

def msg_to_bin(msg):
    if type(msg) == str:
        return ''.join([format(ord(i), "08b") for i in msg])
    return [format(i, "08b") for i in msg]

def show_data(img, length):
    bin_data=""
    for values in img:
        for pixels in values:
            r,g,b=msg_to_bin(pixels)
            bin_data+=r[-1]
            bin_data+=g[-1]
            bin_data+=b[-1]
    allBytes=[bin_data[i: i+8] for i in range(0, len(bin_data), 8)]
    decodedData=""
    for bytes in allBytes:
        decodedData+=chr(int(bytes, 2))
    return decodedData[:-5][:length]

def lsbDetection(path, key, length):
    try:
        image1=imageio.imread(path, as_gray=False, pilmode="RGB")
        data=show_data(image1, length)
        cvz=randomVector(key, length)
        lint=[int(x) for x in list(data)]
        res=pearsonr(cvz, lint)[0]
        return 0 if res>0.5 else 1
    except:
        return -1;
def lsbExtract(path, key, length):
    try:
        image1=imageio.imread(path, as_gray=False, pilmode="RGB")
        data=show_data(image1, length)
        #cvz=randomVector(key, length)
        #res=pearsonr(cvz, list(data))[0]
        return data
    except:
        return "Ошибка при извлечении..."