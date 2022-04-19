import {DefaultUrlSerializer, UrlSerializer, UrlTree} from '@angular/router';

/**
 * Custom UrlSerializer that doesn't escape key-characters such as ';' or '?' etc
 * This is required as in angular (see https://github.com/angular/angular/issues/38706)
 * the default UrlSerializer escapes urls on page-reload.
 */
export class NonescapingUrlserializerClass implements UrlSerializer{

    private ds = new DefaultUrlSerializer();

    parse(url: any): UrlTree{
        const parsed = this.ds.parse(url);
        return parsed;
    }

    serialize(tree: UrlTree): any {
        const serialized = this.ds.serialize(tree);
        return NonescapingUrlserializerClass.revertEscaping(serialized);
    }

    public static revertEscaping(urlLike: string): string{
        return urlLike
            .replace(/%40/gi, '@')
            .replace(/%3A/gi, ':')
            .replace(/%24/gi, '$')
            .replace(/%2C/gi, ',')
            .replace(/%3B/gi, ';')
            .replace(/%20/gi, '+')
            .replace(/%3D/gi, '=')
            .replace(/%3F/gi, '?')
            .replace(/%2F/gi, '/');
    }
}
